package com.android.server.wm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.MiuiMultiWindowUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceControl;
import android.view.ThreadedRenderer;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewRootImpl;
import android.view.WindowManager;
import com.android.server.UiThread;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwLayerSupervisor;
import com.android.server.wm.MiuiCvwPolicy;
import java.util.Objects;
/* loaded from: classes.dex */
public class MiuiCvwCoverLayer implements MiuiCvwLayerSupervisor.IWindowFrameReceiver {
    private static final int BITMAP_MAX_BYTES_THRESHOLD = 50000000;
    private static final int ICON_SHADOW_W_H_THRESHOLD = 800;
    public static final int REQUEST_CREATE_OVERLAYS = 1;
    public static final int REQUEST_ENTERED_MINI = 1024;
    public static final int REQUEST_HIDE_OVERLAYS = 64;
    public static final int REQUEST_NULL = 0;
    public static final int REQUEST_REMOVE_OVERLAYS = 256;
    public static final int REQUEST_SET_CONTENT = 2048;
    public static final int REQUEST_SET_OVERLAYS_CONTENT = 512;
    public static final int REQUEST_SHOW_OVERLAYS = 32;
    public static final int REQUEST_SWITCH_FREEFORM = 2;
    public static final int REQUEST_SWITCH_FULLSCREEN = 16;
    public static final int REQUEST_SWITCH_MINI = 4;
    private Bitmap mBlurContentBitmap;
    private Bitmap mContentBitmap;
    protected final MiuiCvwGestureController mController;
    protected final MiuiCvwAnimator mCvwAnimator;
    private Bitmap mIconBitmap;
    private final LayoutInflater mInflater;
    protected final MiuiCvwLayerSupervisor mLayerSupervisor;
    private volatile MiuiCvwOverlayView mOverlay;
    private WindowManager.LayoutParams mOverlayLayoutParams;
    private ViewRootImpl mOverlaysTargetRoot;
    private MiuiCvwPolicy.TaskWrapperInfo mTaskWrapperInfo;
    private final WindowManager mWindowManager;
    private static final String TAG = MiuiCvwCoverLayer.class.getSimpleName();
    private static final int COVER_MODE_NIGHT_YES_BG_COLOR = Color.parseColor("#000000");
    private static final int COVER_MODE_NIGHT_NO_BG_COLOR = Color.parseColor("#FFFFFF");
    private static final int COVER_NO_BLUR_COLOR = Color.parseColor("#4DFFFFFF");
    private static final int ICON_W_H_THRESHOLD = (int) TypedValue.applyDimension(1, 69.0f, Resources.getSystem().getDisplayMetrics());
    private static final int TEXT_W_THRESHOLD = (int) TypedValue.applyDimension(1, 150.0f, Resources.getSystem().getDisplayMetrics());
    private static final int TEXT_H_THRESHOLD = (int) TypedValue.applyDimension(1, 44.0f, Resources.getSystem().getDisplayMetrics());
    private int mRequestFlags = 0;
    private boolean mIsFullscreenWindow = true;

    public MiuiCvwCoverLayer(MiuiCvwGestureController controller, MiuiCvwLayerSupervisor layerSupervisor, MiuiCvwAnimator cvwAnimator) {
        this.mController = controller;
        this.mLayerSupervisor = layerSupervisor;
        this.mCvwAnimator = cvwAnimator;
        Context context = controller.mWmService.mContext;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        layerSupervisor.register(this);
    }

    public void createCoverLayer(MiuiCvwPolicy.TaskWrapperInfo info) {
        this.mRequestFlags = 0;
        this.mTaskWrapperInfo = info;
        requestCreateOverlays();
        loadOverlaysBitmaps();
    }

    private void loadOverlaysBitmaps() {
        if (this.mTaskWrapperInfo.visualBounds.isEmpty()) {
            return;
        }
        int width = this.mTaskWrapperInfo.visualBounds.width();
        int height = this.mTaskWrapperInfo.visualBounds.height();
        this.mIconBitmap = loadTaskIcon(this.mController.mWmService.mContext, this.mTaskWrapperInfo.packageName);
        Bitmap taskSnapshot = getTaskSnapshot(this.mTaskWrapperInfo);
        this.mContentBitmap = taskSnapshot;
        if (taskSnapshot != null && taskSnapshot.getByteCount() < BITMAP_MAX_BYTES_THRESHOLD) {
            requestSetContent();
            Bitmap bitmap = this.mContentBitmap;
            this.mBlurContentBitmap = loadTaskCover(bitmap, bitmap.getWidth(), this.mContentBitmap.getHeight());
            requestSetOverlays();
            return;
        }
        this.mContentBitmap = getSolidBackground(width, height);
        requestSetContent();
    }

    private void requestCreateOverlays() {
        this.mRequestFlags |= 1;
        this.mLayerSupervisor.requestTraversal(0L);
        this.mLayerSupervisor.addCVWStatus(1);
    }

    private void requestSetOverlays() {
        this.mRequestFlags |= 512;
        this.mLayerSupervisor.requestTraversal(0L);
    }

    private void requestSetContent() {
        this.mRequestFlags |= 2048;
        this.mLayerSupervisor.requestTraversal(0L);
    }

    public void requestShowOverlays() {
        if (isCovering()) {
            return;
        }
        this.mRequestFlags |= 32;
        this.mLayerSupervisor.requestTraversal(0L);
    }

    public void requestHideOverlays() {
        if (isOverlaysHiding()) {
            return;
        }
        this.mRequestFlags |= 64;
        this.mLayerSupervisor.requestTraversal(0L);
    }

    public void notifyFullscreenEntering() {
        this.mRequestFlags |= 16;
    }

    public void notifyMiniEntering() {
        this.mRequestFlags |= 4;
    }

    public void notifyFreeformEntering() {
        this.mRequestFlags |= 2;
    }

    public void notifyEnterMini() {
        this.mRequestFlags |= 1024;
    }

    boolean isGestureWinFreeform() {
        return this.mLayerSupervisor.isGestureWinFreeform();
    }

    boolean isGestureWinFullscreen() {
        return this.mLayerSupervisor.isGestureWinFullscreen();
    }

    boolean isGestureWinMini() {
        return this.mLayerSupervisor.isGestureWinMini();
    }

    boolean isOverlaysComing() {
        return this.mLayerSupervisor.isOverlaysComing();
    }

    public boolean isOverlaysShowFinished() {
        return this.mLayerSupervisor.isOverlaysShowFinished();
    }

    public void taskResized() {
        this.mLayerSupervisor.taskResized();
    }

    public void setTaskResizeWaiting() {
        this.mLayerSupervisor.setTaskResizeWaiting();
    }

    public boolean isTaskResizeWaiting() {
        return this.mLayerSupervisor.taskResizeWaiting();
    }

    public boolean isTaskResized() {
        return this.mLayerSupervisor.isTaskResized();
    }

    public boolean requestPushing() {
        return this.mRequestFlags != 0;
    }

    public void overlaysShowStart() {
        this.mLayerSupervisor.overlaysShowStart();
    }

    public void overlaysShowFinished() {
        this.mLayerSupervisor.overlaysShowFinished();
    }

    public boolean isOverlaysHiding() {
        return this.mLayerSupervisor.isOverlaysHiding();
    }

    public void overlaysHideStart() {
        this.mLayerSupervisor.overlaysHideStart();
    }

    public void overlaysHideFinished() {
        this.mLayerSupervisor.overlaysHideFinished();
        tryRemoveOverlays();
    }

    public void overlaysAttached() {
        ViewRootImpl viewRootImpl = this.mOverlay.getViewRootImpl();
        this.mOverlaysTargetRoot = viewRootImpl;
        this.mCvwAnimator.setViewRootImpl(viewRootImpl);
    }

    public void overlaysDetached() {
        this.mOverlaysTargetRoot = null;
        this.mCvwAnimator.setViewRootImpl(null);
    }

    public void resizeTask() {
        this.mLayerSupervisor.preResizeTask();
    }

    public SurfaceControl showTask() {
        return this.mLayerSupervisor.showTask();
    }

    public SurfaceControl hideTask() {
        return this.mLayerSupervisor.hideTask();
    }

    public void requestRemoveOverlays() {
        MiuiCvwGestureController.Slog.d(TAG, "requestRemoveOverlays");
        this.mRequestFlags |= 256;
        this.mLayerSupervisor.requestTraversal(0L);
    }

    public void remove(long delay) {
        this.mLayerSupervisor.mExecuteHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwCoverLayer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwCoverLayer.this.requestHideOverlays();
            }
        }, delay);
    }

    public void remove() {
        requestHideOverlays();
    }

    public boolean isCovering() {
        return this.mLayerSupervisor.isCovering();
    }

    public boolean isMiniMode() {
        return this.mLayerSupervisor.isGestureWinMini();
    }

    private Bitmap loadTaskIcon(Context context, String pkg) {
        if (context == null || TextUtils.isEmpty(pkg)) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            return zoomDrawableIfNeed(packageManager.getApplicationIcon(pkg));
        } catch (Exception e) {
            MiuiCvwGestureController.Slog.e(TAG, "loadTaskIcon err ", e);
            return null;
        }
    }

    private Bitmap zoomDrawableIfNeed(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
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
        return Bitmap.createBitmap(output, 0, 0, width, height, matrix, true);
    }

    protected Bitmap getTaskSnapshot(MiuiCvwPolicy.TaskWrapperInfo info) {
        this.mIsFullscreenWindow = true;
        MiuiFreeFormActivityStack stack = info.stack;
        if (stack == null) {
            MiuiCvwGestureController.Slog.d(TAG, "stack is empty,return null.");
            return null;
        }
        Task task = stack.mTask;
        Rect visualBounds = info.visualBounds;
        WindowState w = task.getTopVisibleAppMainWindow();
        if (w == null) {
            MiuiCvwGestureController.Slog.d(TAG, "task contains secure child.");
            return null;
        }
        if (!task.getBounds().equals(w.getBounds())) {
            MiuiCvwGestureController.Slog.d(TAG, "window bounds is not full");
            this.mIsFullscreenWindow = false;
        }
        Rect sourceCrop = new Rect();
        float f = 1.0f;
        sourceCrop.set(0, 0, (int) (((visualBounds.width() * 1.0f) / info.visualScale) + 0.5f), (int) (((visualBounds.height() * 1.0f) / info.visualScale) + 0.5f));
        sourceCrop.offsetTo(0, 0);
        String str = TAG;
        MiuiCvwGestureController.Slog.d(str, "load task snapshot,bounds:" + sourceCrop);
        if (this.mLayerSupervisor.isGestureWinFullscreen()) {
            f = 0.3f;
        }
        float captureScale = f;
        SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = createSnapshotBuffer(task.getSurfaceControl(), sourceCrop, captureScale);
        Bitmap bitmap = screenshotBuffer == null ? null : screenshotBuffer.asBitmap();
        if (bitmap == null) {
            MiuiCvwGestureController.Slog.w(str, "Failed to take screenshot");
            return null;
        }
        Bitmap ret = bitmap.asShared();
        if (ret != bitmap) {
            bitmap.recycle();
        }
        return ret;
    }

    private Bitmap getSolidBackground(int width, int height) {
        Bitmap solidColorBit = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(solidColorBit);
        if (MiuiMultiWindowUtils.isNightMode(this.mController.mWmService.mContext)) {
            canvas.drawColor(COVER_MODE_NIGHT_YES_BG_COLOR);
            canvas.drawColor(COVER_NO_BLUR_COLOR);
        } else {
            canvas.drawColor(COVER_MODE_NIGHT_NO_BG_COLOR);
            canvas.drawColor(COVER_NO_BLUR_COLOR);
        }
        return solidColorBit;
    }

    private static SurfaceControl.ScreenshotHardwareBuffer createSnapshotBuffer(SurfaceControl target, Rect bounds, float frameScale) {
        Rect cropBounds = new Rect();
        if (bounds != null) {
            cropBounds.set(bounds);
            cropBounds.offsetTo(0, 0);
        }
        SurfaceControl.LayerCaptureArgs captureArgs = new SurfaceControl.LayerCaptureArgs.Builder(target).setSourceCrop(cropBounds).setFrameScale(frameScale).setCaptureSecureLayers(true).setAllowProtected(true).build();
        return SurfaceControl.captureLayers(captureArgs);
    }

    protected Bitmap loadTaskCover(Bitmap taskSnapshot, int width, int height) {
        Bitmap in = taskSnapshot.copy(Bitmap.Config.HARDWARE, false);
        RenderNode node = new RenderNode("blurNode");
        node.setRenderEffect(RenderEffect.createBlurEffect(130.0f, 130.0f, Shader.TileMode.CLAMP));
        node.setPosition(0, 0, width, height);
        RecordingCanvas canvas = node.start(width, height);
        canvas.drawBitmap(in, new Rect(0, 0, width, height), new Rect(0, 0, width, height), (Paint) null);
        node.end(canvas);
        return ThreadedRenderer.createHardwareBitmap(node, width, height);
    }

    private void setupBlendMode(int color, Paint blendPaint, BlendMode blendMode) {
        blendPaint.setColor(color);
        if (Build.VERSION.SDK_INT >= 29) {
            blendPaint.setBlendMode(blendMode);
        }
    }

    public float getMiniFreeformRadius() {
        return this.mLayerSupervisor.getFreeformRadius(true);
    }

    public float getFreeformRadius() {
        return this.mLayerSupervisor.getFreeformRadius(false);
    }

    public float getRadius() {
        return this.mLayerSupervisor.computeFreeformRadius(isGestureWinMini(), 1.0f);
    }

    @Override // com.android.server.wm.MiuiCvwLayerSupervisor.IWindowFrameReceiver
    public void doFrame(int l, int t, int r, int b) {
        handleOverlaysRequest();
    }

    private void handleOverlaysRequest() {
        int i = this.mRequestFlags;
        if (i == 0) {
            return;
        }
        if ((i & 1) != 0) {
            this.mRequestFlags = i & (-2);
            MiuiCvwGestureController.Slog.d(TAG, "doFrame  createOverlays");
            applyCreateOverlaysWindow();
        }
        synchronized (MiuiCvwCoverLayer.class) {
            if (!overlaysValid()) {
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  overlays is unavailable, isCovering ：" + isCovering());
                return;
            }
            int i2 = this.mRequestFlags;
            if ((i2 & 512) != 0) {
                this.mRequestFlags = i2 & (-513);
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  setOverlaysContent");
                applySetOverlaysContent();
            }
            if (overlaysValid()) {
                int i3 = this.mRequestFlags;
                if ((i3 & 2048) != 0) {
                    this.mRequestFlags = i3 & (-2049);
                    MiuiCvwGestureController.Slog.d(TAG, "doFrame  setContent");
                    applySetContent();
                }
            }
            int i4 = this.mRequestFlags;
            if ((i4 & 64) != 0) {
                this.mRequestFlags = i4 & (-65);
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  hideOverlays");
                applyHideOverlays();
            }
            int i5 = this.mRequestFlags;
            if ((i5 & 256) != 0) {
                this.mRequestFlags = 0;
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  removeOverlaysWindow");
                applyRemoveOverlaysWindow();
                return;
            }
            if ((i5 & 32) != 0 && this.mOverlay.prepareToShow()) {
                this.mRequestFlags &= -33;
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  showOverlays");
                applyShowOverlays();
            }
            if ((this.mRequestFlags & 2) != 0 && this.mOverlay.prepareToShow()) {
                this.mRequestFlags &= -3;
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  freeformEntering");
                applySwitchFreeform();
            }
            if ((this.mRequestFlags & 16) != 0 && this.mOverlay.prepareToShow()) {
                this.mRequestFlags &= -17;
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  fullscreenEntering");
                applySwitchFullscreen();
            }
            int i6 = this.mRequestFlags;
            if ((i6 & 4) != 0) {
                this.mRequestFlags = i6 & (-5);
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  miniEntering");
                applySwitchMini();
            }
            int i7 = this.mRequestFlags;
            if ((i7 & 1024) != 0) {
                this.mRequestFlags = i7 & (-1025);
                MiuiCvwGestureController.Slog.d(TAG, "doFrame  miniEntering");
                applyEnterMini();
            }
        }
    }

    private boolean overlaysValid() {
        if (this.mOverlay != null) {
            ViewRootImpl viewRoot = this.mOverlay.getViewRootImpl();
            boolean hasSurface = viewRoot != null && viewRoot.getSurfaceControl().isValid();
            return hasSurface;
        }
        return false;
    }

    public ViewRootImpl getOverlaysTargetRoot() {
        return this.mOverlaysTargetRoot;
    }

    private void applyCreateOverlaysWindow() {
        try {
            if (this.mOverlay == null) {
                this.mOverlay = (MiuiCvwOverlayView) this.mInflater.inflate(285999117, (ViewGroup) null);
                this.mOverlay.init(this);
                this.mOverlayLayoutParams = createLayoutParams();
                this.mWindowManager.addView(this.mOverlay, this.mOverlayLayoutParams);
                this.mLayerSupervisor.addCVWStatus(2);
            }
        } catch (Exception e) {
            MiuiCvwGestureController.Slog.e(TAG, "applyCreateOverlaysWindow err ", e);
            this.mOverlay = null;
        }
    }

    public boolean windowIsFullScreen() {
        return this.mIsFullscreenWindow;
    }

    public float getMaxRefrashRate() {
        float maxFrameRate = MiuiFreeformPinManagerService.EDGE_AREA;
        Display display = this.mLayerSupervisor.mController.mDisplayContent.getDisplay();
        Display.Mode mode = display.getMode();
        float[] refreshRates = mode.getAlternativeRefreshRates();
        for (float refreshRate : refreshRates) {
            if (refreshRate > maxFrameRate) {
                maxFrameRate = refreshRate;
            }
        }
        return maxFrameRate;
    }

    private WindowManager.LayoutParams createLayoutParams() {
        int width = MiuiCvwPolicy.mCurrScreenWidth;
        int height = MiuiCvwPolicy.mCurrScreenHeight;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, 2021, 16779032, 1);
        lp.privateFlags |= 536870912;
        lp.privateFlags |= 16;
        lp.privateFlags |= 64;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.gravity = 51;
        lp.y = 0;
        lp.x = 0;
        lp.setTitle("cvw-overlay");
        return lp;
    }

    private void applySetOverlaysContent() {
        this.mOverlay.setBlurContent(this.mBlurContentBitmap);
    }

    private void applySetContent() {
        this.mOverlay.setContent(this.mIconBitmap, this.mContentBitmap);
    }

    private void applyHideOverlays() {
        if (isCovering() && this.mOverlay != null) {
            this.mOverlay.hide();
        }
    }

    private void applyRemoveOverlaysWindow() {
        Runnable runnable;
        Handler handler;
        try {
            if (this.mOverlay != null) {
                this.mOverlay.reset();
                ViewParent viewParent = this.mOverlay.getRootView().getParent();
                if (viewParent != null) {
                    this.mWindowManager.removeView(this.mOverlay);
                }
            }
            this.mOverlay = null;
            this.mOverlayLayoutParams = null;
            this.mLayerSupervisor.addCVWStatus(2048);
            handler = UiThread.getHandler();
            final MiuiCvwLayerSupervisor miuiCvwLayerSupervisor = this.mLayerSupervisor;
            Objects.requireNonNull(miuiCvwLayerSupervisor);
            runnable = new Runnable() { // from class: com.android.server.wm.MiuiCvwCoverLayer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwLayerSupervisor.this.removeTaskLeashUi();
                }
            };
        } catch (Exception e) {
            this.mOverlay = null;
            this.mOverlayLayoutParams = null;
            this.mLayerSupervisor.addCVWStatus(2048);
            handler = UiThread.getHandler();
            final MiuiCvwLayerSupervisor miuiCvwLayerSupervisor2 = this.mLayerSupervisor;
            Objects.requireNonNull(miuiCvwLayerSupervisor2);
            runnable = new Runnable() { // from class: com.android.server.wm.MiuiCvwCoverLayer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwLayerSupervisor.this.removeTaskLeashUi();
                }
            };
        } catch (Throwable th) {
            this.mOverlay = null;
            this.mOverlayLayoutParams = null;
            this.mLayerSupervisor.addCVWStatus(2048);
            Handler handler2 = UiThread.getHandler();
            final MiuiCvwLayerSupervisor miuiCvwLayerSupervisor3 = this.mLayerSupervisor;
            Objects.requireNonNull(miuiCvwLayerSupervisor3);
            handler2.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwCoverLayer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwLayerSupervisor.this.removeTaskLeashUi();
                }
            });
            throw th;
        }
        handler.post(runnable);
    }

    private void applyShowOverlays() {
        this.mOverlay.show();
    }

    private void applySwitchFreeform() {
        if (!isCovering()) {
            return;
        }
        this.mOverlay.switchFreeform();
    }

    private void applySwitchFullscreen() {
        if (this.mOverlay == null) {
            requestCreateOverlays();
            notifyFullscreenEntering();
            return;
        }
        this.mOverlay.switchFullScreen();
    }

    private void applySwitchMini() {
        if (this.mOverlay == null) {
            requestCreateOverlays();
            notifyMiniEntering();
            return;
        }
        this.mOverlay.switchMini();
        this.mLayerSupervisor.addCVWStatus(8);
    }

    private void applyEnterMini() {
        this.mOverlay.enterMini();
    }

    public void applyWindowFrame(int l, int t, int r, int b) {
        if (this.mOverlay != null && overlaysValid()) {
            this.mOverlay.setFrame(l, t, r, b);
        }
    }

    public void tryRemoveOverlays() {
        this.mLayerSupervisor.addCVWStatus(1024);
        if (this.mLayerSupervisor.isAllAnimalFinished() && (!this.mLayerSupervisor.isCovering() || (this.mLayerSupervisor.isCovering() && this.mLayerSupervisor.isOverlaysHideFinished()))) {
            requestRemoveOverlays();
        } else {
            MiuiCvwGestureController.Slog.d(TAG, "tryRemoveOverlays isCovering:" + isCovering() + ",isOverlaysHideFinished:" + this.mLayerSupervisor.isOverlaysHideFinished());
        }
    }
}
