package com.android.server.wm;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.xiaomi.screenprojection.IMiuiScreenProjectionStub;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import miui.app.MiuiFreeFormManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiFreeformPinManagerService {
    public static final float CLICK_GAP = 28.0f;
    public static final float CORNER_AREA = 163.27f;
    public static final float EDGE_AREA = 0.0f;
    public static final float FLOATING_WINDOW_CORNER_RADIUS = 18.18f;
    public static final float FLOATING_WINDOW_GAP = 6.0f;
    public static final float FLOATING_WINDOW_HEIGHT = 64.0f;
    public static final float FLOATING_WINDOW_ICON_HEIGHT = 56.0f;
    public static final float FLOATING_WINDOW_X_OFFSET = 24.0f;
    public static final float GAP = 35.0f;
    public static final float MINI_GAP = 26.18f;
    public static final float OUTER_AREA = 145.45f;
    public static final float SIDE_AREA = 145.45f;
    public static final float SLIDE_OUT_POSITION_THRESHOLD_VERSION_TWO = 236.36f;
    public static final float SLOW_SPEED = 545.45f;
    public static final float SMALLER_CORNER_AREA = 36.36f;
    public static final float STOP_SPEED = 363.64f;
    private static final String TAG = "MiuiFreeformPinManagerService";
    MiuiFreeFormGestureController mController;
    Handler mHandler;
    private final Map<Integer, MiuiFreeFormFloatIconInfo> mMiuiFreeFormFloatIconInfos = new ConcurrentHashMap(2);
    Object mLock = new Object();
    private int mPinedFreeformCount = 0;
    private int mPinedMiniFreeformCount = 0;

    public MiuiFreeformPinManagerService(MiuiFreeFormGestureController controller, Looper looper) {
        this.mController = controller;
        this.mHandler = new H(looper);
    }

    public static boolean isOutsideScreen(float x, float y, float predictX, float predictY, int screenWidth, int screenHeight) {
        boolean isOut = predictX < (-MiuiMultiWindowUtils.applyDip2Px(145.45f)) || predictY < (-MiuiMultiWindowUtils.applyDip2Px(145.45f)) || predictX > ((float) screenWidth) + MiuiMultiWindowUtils.applyDip2Px(145.45f) || predictY > ((float) screenHeight) + MiuiMultiWindowUtils.applyDip2Px(145.45f);
        if (isOut) {
            boolean adjust = false;
            float targetX = x;
            float disY = predictY - y;
            float disX = predictX - x;
            float edge = EDGE_AREA;
            if (predictY < EDGE_AREA) {
                edge = EDGE_AREA;
                adjust = true;
            } else if (predictY > screenHeight) {
                edge = screenHeight;
                adjust = true;
            }
            if (adjust) {
                if (disY != EDGE_AREA) {
                    float k = disX / disY;
                    targetX = x + ((edge - y) * k);
                }
                if (targetX < MiuiMultiWindowUtils.applyDip2Px(163.27f) || targetX > screenWidth - MiuiMultiWindowUtils.applyDip2Px(163.27f)) {
                    return true;
                }
                return false;
            }
            return isOut;
        }
        return isOut;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public final class H extends Handler {
        static final int HIDE_PIN_FLOATING_WINDOW_DELAY = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        H(Looper looper) {
            super(looper);
            MiuiFreeformPinManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiFreeformPinManagerService.this.hideStack((MiuiFreeFormActivityStack) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    public boolean handleFreeFormToPin(MiuiFreeFormActivityStack mffas) {
        if (mffas == null || mffas.mTask == null || mffas.inPinMode()) {
            Slog.d(TAG, "skip handleFreeFormToPin mffas: " + mffas);
            return false;
        }
        initPinFloatingWindowPos(mffas);
        setPinFloatingWindowAnimationInfo(mffas, true);
        return true;
    }

    private void initPinFloatingWindowPos(MiuiFreeFormActivityStack mffas) {
        Rect realBounds;
        int left;
        if (mffas.isInMiniFreeFormMode()) {
            realBounds = mffas.mStackControlInfo.mSmallWindowBounds;
        } else {
            realBounds = getRealTaskBounds(mffas);
        }
        if (realBounds.centerX() > this.mController.mGestureListener.mScreenWidth / 2) {
            left = (int) ((this.mController.mGestureListener.mScreenWidth - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f);
        } else {
            left = (int) (-((((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f));
        }
        int top = realBounds.top;
        Rect pinFloatingWindowPos = new Rect(left, top, ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) + left, ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) + top);
        adjustFloatingWindowPosIfNeed(this.mController.mMiuiFreeFormManagerService.getCurrPinFloatingWindowPos(true, true), pinFloatingWindowPos);
        mffas.mPinFloatingWindowPos.set(pinFloatingWindowPos);
        mffas.mIsPinFloatingWindowPosInit = true;
        Slog.i(TAG, "initPinFloatingWindowPos:" + pinFloatingWindowPos);
    }

    public ArrayList<Rect> getSidebarLineRects() {
        ArrayList<Rect> sidebarLineRects = new ArrayList<>();
        String sidebarBounds = Settings.Secure.getString(this.mController.mService.mContext.getContentResolver(), "sidebar_bounds");
        if (sidebarBounds == null || "".equals(sidebarBounds)) {
            return sidebarLineRects;
        }
        try {
            JSONArray sidebarArray = new JSONArray(sidebarBounds);
            for (int i = 0; i < sidebarArray.length(); i++) {
                JSONObject bound = sidebarArray.optJSONObject(i);
                if (bound != null) {
                    int left = bound.optInt("l", -1);
                    int top = bound.optInt("t", -1);
                    int right = bound.optInt(FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST, -1);
                    int bottom = bound.optInt(FoldablePackagePolicy.POLICY_VALUE_BLOCK_LIST, -1);
                    Rect sidebarRect = new Rect();
                    sidebarRect.set(left, top, right, bottom);
                    sidebarLineRects.add(sidebarRect);
                }
            }
        } catch (JSONException e) {
            Slog.e(TAG, "getSidebarLineRects");
        }
        Slog.i(TAG, "getSidebarLineRects: " + sidebarLineRects);
        return sidebarLineRects;
    }

    public void adjustFloatingWindowPosIfNeed(List<Rect> currPinFloatingWindowPoses, Rect initPos) {
        Slog.d(TAG, "adjustFloatingWindowPosIfNeed: " + currPinFloatingWindowPoses + "initPos: " + initPos);
        boolean firstIntersect = true;
        for (int i = 0; i < currPinFloatingWindowPoses.size(); i++) {
            Rect currPinFloatingWindowPos = currPinFloatingWindowPoses.get(i);
            Rect expandRect = new Rect(currPinFloatingWindowPos);
            expandRect.inset(0, -(((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f)) - 1));
            if (Rect.intersects(expandRect, initPos)) {
                if (firstIntersect && initPos.top < currPinFloatingWindowPos.top && (currPinFloatingWindowPos.top - ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f))) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) >= MiuiMultiWindowUtils.applyDip2Px(35.0f)) {
                    initPos.offsetTo(initPos.left, (currPinFloatingWindowPos.top - ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f))) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)));
                    Slog.d(TAG, "top adjust first initPos: " + initPos);
                } else if (currPinFloatingWindowPos.bottom + ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f)) + ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) <= this.mController.mGestureListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) {
                    initPos.offsetTo(initPos.left, currPinFloatingWindowPos.bottom + ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f)));
                    Slog.d(TAG, "bottom adjust initPos: " + initPos);
                }
                firstIntersect = false;
            }
        }
        int i2 = currPinFloatingWindowPoses.size();
        for (int i3 = i2 - 1; i3 >= 0; i3--) {
            Rect currPinFloatingWindowPos2 = currPinFloatingWindowPoses.get(i3);
            Rect expandRect2 = new Rect(currPinFloatingWindowPos2);
            expandRect2.inset(0, -(((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f)) - 1));
            if (Rect.intersects(expandRect2, initPos)) {
                initPos.offsetTo(initPos.left, (currPinFloatingWindowPos2.top - ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f))) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)));
                Slog.d(TAG, "top adjust second initPos: " + initPos);
            }
        }
        int i4 = initPos.top;
        if (i4 - ((int) (MiuiMultiWindowUtils.applyDip2Px(6.0f) + 0.5f)) < MiuiMultiWindowUtils.applyDip2Px(35.0f)) {
            initPos.offsetTo(initPos.left, (int) MiuiMultiWindowUtils.applyDip2Px(35.0f));
            Slog.d(TAG, "get max place initPos: " + initPos);
        }
    }

    public void updatePinFloatingWindowPos(MiuiFreeFormActivityStack mffas, Rect rect) {
        updatePinFloatingWindowPos(mffas, rect, false);
    }

    public void updatePinFloatingWindowPos(MiuiFreeFormActivityStack mffas, Rect rect, boolean isMovePinPos) {
        if (rect == null) {
            return;
        }
        if (isMovePinPos && !rect.isEmpty() && this.mController.mTrackManager != null && !rect.equals(mffas.mPinFloatingWindowPos)) {
            if (mffas.isInFreeFormMode()) {
                this.mController.mTrackManager.trackSmallWindowPinedMoveEvent(mffas.getStackPackageName(), mffas.getApplicationName(), rect.left, rect.top);
            } else if (mffas.isInMiniFreeFormMode()) {
                this.mController.mTrackManager.trackMiniWindowPinedMoveEvent(mffas.getStackPackageName(), mffas.getApplicationName(), rect.left, rect.top);
            }
        }
        mffas.mPinFloatingWindowPos.set(rect);
        if (mffas.mPinFloatingWindowPos.isEmpty()) {
            mffas.mIsPinFloatingWindowPosInit = false;
        } else if (!mffas.mIsPinFloatingWindowPosInit) {
            mffas.mIsPinFloatingWindowPosInit = true;
        }
        Slog.i(TAG, "update floating ball position:" + rect + "mffas:" + mffas);
    }

    public boolean unPinFloatingWindow(final MiuiFreeFormActivityStack mffas, final float upVelocityX, final float upVelocityY, final boolean isClick) {
        Slog.i(TAG, "unPinFloatingWindow mffas: " + mffas + " upVelocityX: " + upVelocityX + " upVelocityY: " + upVelocityY);
        if (mffas == null || mffas.mTask == null || !mffas.mTask.inFreeformWindowingMode() || !mffas.inPinMode() || mffas.mIsRunningPinAnim || !mffas.mIsPinFloatingWindowPosInit || mffas.mIsRunningUnPinAnim) {
            Slog.d(TAG, "skip unPinFloatingWindow mffas: " + mffas);
            return false;
        }
        final MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo = getFloatIconInfo(mffas);
        if (miuiFreeFormFloatIconInfo.getIconBitmap() == null) {
            Slog.d(TAG, "skip unPinFloatingWindow getIconBitmap == null mffas: " + mffas);
            return false;
        } else if (mffas.mTask.getTopNonFinishingActivity() == null) {
            Slog.d(TAG, "skip unPinFloatingWindow getTopNonFinishingActivity == null mffas: " + mffas);
            return false;
        } else if (mffas.mTask.getDisplayContent() == null) {
            Slog.d(TAG, "skip unPinFloatingWindow getDisplayContent == null mffas: " + mffas);
            return false;
        } else {
            final DisplayContent displayContent = mffas.mTask.getDisplayContent();
            if (mffas.getPinMode() == 1) {
                this.mController.mGestureListener.mGestureAnimator.showStack(mffas);
            } else if (mffas.getPinMode() == 2) {
                this.mController.mGestureListener.mGestureAnimator.hideStack(mffas);
                this.mController.mGestureListener.mGestureAnimator.applyTransaction();
                this.mController.moveTaskToFront(mffas);
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeformPinManagerService$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiFreeformPinManagerService.this.m1753xcefafc23(mffas, upVelocityX, upVelocityY, isClick, displayContent, miuiFreeFormFloatIconInfo);
                }
            });
            return true;
        }
    }

    /* renamed from: lambda$unPinFloatingWindow$0$com-android-server-wm-MiuiFreeformPinManagerService */
    public /* synthetic */ void m1753xcefafc23(MiuiFreeFormActivityStack mffas, float upVelocityX, float upVelocityY, boolean isClick, DisplayContent displayContent, MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo) {
        long timeoutAtTimeMs = System.currentTimeMillis() + 2000;
        synchronized (this.mLock) {
            try {
                mffas.topWindowHasDrawn = false;
                while (!mffas.topWindowHasDrawn) {
                    try {
                        long waitMillis = timeoutAtTimeMs - System.currentTimeMillis();
                        if (waitMillis <= 0) {
                            break;
                        }
                        this.mLock.wait(waitMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        ActivityRecord activityRecord = mffas.mTask.getTopNonFinishingActivity();
        Slog.i(TAG, "unPinFloatingWindow mffas: " + mffas + " activityRecord: " + activityRecord);
        if (activityRecord == null) {
            return;
        }
        mffas.mEnterVelocityX = upVelocityX;
        mffas.mEnterVelocityY = upVelocityY;
        mffas.mIsEnterClick = isClick;
        mffas.mIsPinFloatingWindowPosInit = false;
        setUpMiuiFreeWindowFloatIconAnimation(mffas, activityRecord, displayContent, miuiFreeFormFloatIconInfo);
        startUnPinAnimation(mffas);
    }

    public void notifyHasDrawn(MiuiFreeFormActivityStack mffas, WindowState windowState) {
        WindowState mainWindow;
        ActivityRecord activityRecord = mffas.mTask.getTopNonFinishingActivity();
        synchronized (this.mController.mService.mGlobalLock) {
            mainWindow = activityRecord.findMainWindow();
        }
        synchronized (this.mLock) {
            if (mainWindow != null) {
                if (windowState.equals(mainWindow) && !mffas.topWindowHasDrawn) {
                    Slog.i(TAG, "notifyHasDrawn: " + mffas + "windowState: " + windowState);
                    mffas.topWindowHasDrawn = true;
                    this.mLock.notifyAll();
                }
            }
        }
    }

    public void hidePinFloatingWindow(MiuiFreeFormActivityStack mffas) {
        if (mffas == null) {
            return;
        }
        Slog.i(TAG, "hidePinFloatingWindow mffas: " + mffas + "mffas.inPinMode(): " + mffas.inPinMode() + "mHandler.hasMessages(HIDE_PIN_FLOATING_WINDOW_DELAY, mffas): " + this.mHandler.hasMessages(1, mffas));
        if (mffas.inPinMode() && this.mHandler.hasMessages(1, mffas)) {
            this.mHandler.removeMessages(1, mffas);
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(1, mffas));
        }
    }

    public void hidePinFloatingWindowDelay(MiuiFreeFormActivityStack mffas, int delay) {
        if (mffas == null) {
            return;
        }
        Slog.i(TAG, "hidePinFloatingWindowDelay mffas: " + mffas + "mffas.inPinMode(): " + mffas.inPinMode() + " delay: " + delay);
        if (mffas.inPinMode()) {
            this.mHandler.removeMessages(1, mffas);
            Handler handler = this.mHandler;
            handler.sendMessageDelayed(handler.obtainMessage(1, mffas), delay);
        }
    }

    public void hideStack(MiuiFreeFormActivityStack stack) {
        WindowState mainWindow;
        Slog.i(TAG, "hideStack stack: " + stack);
        this.mController.mGestureListener.mGestureAnimator.hideStack(stack);
        this.mController.mGestureListener.mGestureAnimator.applyTransaction();
        this.mController.moveTaskToBack(stack);
        ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
        synchronized (this.mController.mService.mGlobalLock) {
            if (activityRecord != null) {
                if (activityRecord.mFloatWindwoIconSurfaceControl != null && (mainWindow = activityRecord.findMainWindow()) != null) {
                    this.mController.mGestureListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 1.0f);
                    this.mController.mGestureListener.mGestureAnimator.applyTransaction();
                }
            }
        }
        clearMiuiFreeWindowFloatIconLayer(stack);
        if (!stack.isInMiniFreeFormMode()) {
            this.mController.mGestureListener.mGestureAnimator.removeAnimationControlLeash(stack);
        } else {
            this.mController.mGestureListener.mGestureAnimator.setWindowCropInTransaction(stack, null);
        }
        stack.updateCornerRadius();
        final MiuiFreeFormManagerService service = this.mController.mMiuiFreeFormManagerService;
        TaskDisplayArea defaultTaskDisplayArea = service.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
        if (defaultTaskDisplayArea != null) {
            synchronized (this.mController.mGestureListener.mService.mGlobalLock) {
                Task focusableTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeformPinManagerService$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return MiuiFreeFormManagerService.this.getMiuiFreeFormActivityStackForMiuiFB(((Task) obj).getRootTaskId());
                    }
                });
                if (focusableTask != null) {
                    this.mController.mGestureListener.mService.mAtmService.setFocusedTask(focusableTask.getRootTaskId());
                    this.mController.mGestureListener.mService.updateFocusedWindowLocked(0, true);
                }
            }
        }
        stack.mIsRunningPinAnim = false;
    }

    public void setPinFloatingWindowAnimationInfo(MiuiFreeFormActivityStack mffas, boolean startPinAnimation) {
        if (mffas == null || mffas.mTask == null || mffas.mTask.topRunningActivity() == null) {
            return;
        }
        ActivityRecord activityRecord = mffas.mTask.topRunningActivity();
        if (mffas.mPinFloatingWindowPos.isEmpty()) {
            Slog.w(TAG, "PinFloatingWindow position is uninitialized, mffas:" + mffas);
            return;
        }
        String packageName = activityRecord.packageName;
        ComponentName realActivity = activityRecord.task == null ? null : activityRecord.task.realActivity;
        if (realActivity != null) {
            packageName = realActivity.getPackageName();
        }
        setAppWindowExitInfo(mffas, MiuiMultiWindowUtils.drawableToBitmap(getAppIconForFloatingBall(this.mController.mService.mContext, packageName, activityRecord.mUserId)), startPinAnimation);
        Slog.i(TAG, "setFloatingBallAnimationInfo, record:" + activityRecord + ", pos:" + mffas.mPinFloatingWindowPos);
    }

    public static Drawable getAppIconForFloatingBall(Context context, String packageName, int userId) {
        if (context == null) {
            return null;
        }
        Drawable drawable = getRawAppIcon(context, packageName, userId, true);
        Bitmap rawBitmap = MiuiMultiWindowUtils.drawableToBitmap(drawable);
        if (rawBitmap == null) {
            Slog.e(TAG, "get round app icon error!");
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(rawBitmap.getWidth(), rawBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        BitmapShader bitmapShader = new BitmapShader(rawBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setShader(bitmapShader);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRoundRect(new RectF(EDGE_AREA, EDGE_AREA, rawBitmap.getWidth(), rawBitmap.getHeight()), EDGE_AREA, EDGE_AREA, paint);
        return bitmap2Drawable(bitmap);
    }

    private static Drawable getRawAppIcon(Context context, String packageName, int userId, boolean handleXSpaceUser) {
        PackageManager packageManager;
        ApplicationInfo applicationInfo = null;
        Drawable drawable = null;
        Drawable handleXSpaceUserdrawable = null;
        if (packageName == null || packageName.isEmpty() || (packageManager = context.getPackageManager()) == null) {
            return null;
        }
        try {
            applicationInfo = packageManager.getApplicationInfoAsUser(packageName, 0, userId);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "icon not load!");
        }
        if (applicationInfo != null) {
            drawable = applicationInfo.loadIcon(packageManager);
        }
        if (drawable == null) {
            Slog.w(TAG, "getAppIcon failed, cause iconDrawable is null!");
            return drawable;
        }
        UserHandle userHandle = new UserHandle(userId);
        if (handleXSpaceUser) {
            handleXSpaceUserdrawable = packageManager.getUserBadgedIcon(drawable, userHandle);
        }
        if (handleXSpaceUserdrawable == null) {
            Drawable handleXSpaceUserdrawable2 = drawable;
            return handleXSpaceUserdrawable2;
        }
        return handleXSpaceUserdrawable;
    }

    public static Drawable bitmap2Drawable(Bitmap bitmap) {
        return new BitmapDrawable(bitmap);
    }

    public void setAppWindowExitInfo(MiuiFreeFormActivityStack mffas, Bitmap bitmap, boolean startPinAnimation) {
        if (mffas == null || bitmap == null) {
            return;
        }
        mffas.mExitIconWidth = bitmap.getWidth();
        mffas.mExitIconHeight = bitmap.getHeight();
        mffas.mExitPivotX = mffas.mPinFloatingWindowPos.centerX();
        mffas.mExitPivotY = mffas.mPinFloatingWindowPos.centerY();
        mffas.mExitIconBitmap = bitmap;
        pushFloatIconInfoToMap(mffas);
        startAnimationForFreeFormGestureNavigation(mffas, startPinAnimation);
    }

    private void pushFloatIconInfoToMap(MiuiFreeFormActivityStack mffas) {
        MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo = new MiuiFreeFormFloatIconInfo();
        miuiFreeFormFloatIconInfo.setFloatPivotWidth(mffas.mExitIconWidth);
        miuiFreeFormFloatIconInfo.setFloatPivotHeight(mffas.mExitIconHeight);
        miuiFreeFormFloatIconInfo.setFloatPivotX(Float.valueOf(mffas.mExitPivotX));
        miuiFreeFormFloatIconInfo.setFloatPivotY(Float.valueOf(mffas.mExitPivotY));
        miuiFreeFormFloatIconInfo.setFloatVelocityX(Float.valueOf(mffas.mExitVelocityX));
        miuiFreeFormFloatIconInfo.setFloatVelocityY(Float.valueOf(mffas.mExitVelocityY));
        miuiFreeFormFloatIconInfo.setTaskId(mffas.mTask.mTaskId);
        Bitmap bitmap = mffas.mExitIconBitmap;
        if (bitmap != null) {
            miuiFreeFormFloatIconInfo.setIconBitmap(Bitmap.createBitmap(bitmap));
            this.mMiuiFreeFormFloatIconInfos.put(Integer.valueOf(mffas.mTask.mTaskId), miuiFreeFormFloatIconInfo);
        }
    }

    public void resetAppWindowExitInfo(MiuiFreeFormActivityStack mffas) {
        if (this.mMiuiFreeFormFloatIconInfos.size() > 0) {
            this.mMiuiFreeFormFloatIconInfos.remove(Integer.valueOf(mffas.mLastFloatIconlayerTaskId));
            mffas.mExitPivotX = -1.0f;
            mffas.mExitPivotY = -1.0f;
            mffas.mExitVelocityX = EDGE_AREA;
            mffas.mExitVelocityY = EDGE_AREA;
            mffas.mExitIconBitmap = null;
            mffas.mExitIconWidth = -1;
            mffas.mExitIconHeight = -1;
            mffas.mLastTokenLayer = -1;
            mffas.mLastIconLayerWindowToken = null;
            mffas.mEnterVelocityX = EDGE_AREA;
            mffas.mEnterVelocityY = EDGE_AREA;
            mffas.mIsEnterClick = false;
            Slog.i(TAG, "floating window icon info has been reset.");
        }
    }

    private void startAnimationForFreeFormGestureNavigation(MiuiFreeFormActivityStack mffas, boolean startPinAnimation) {
        ActivityRecord activityRecord;
        DisplayContent displayContent;
        if (mffas.mTask == null || mffas.mTask.getTopNonFinishingActivity() == null || !mffas.mTask.inFreeformWindowingMode() || (displayContent = (activityRecord = mffas.mTask.getTopNonFinishingActivity()).getDisplayContent()) == null) {
            return;
        }
        MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo = getFloatIconInfo(mffas);
        setUpMiuiFreeWindowFloatIconAnimation(mffas, activityRecord, displayContent, miuiFreeFormFloatIconInfo);
        if (startPinAnimation) {
            startPinAnimation(mffas);
        }
    }

    private void startPinAnimation(MiuiFreeFormActivityStack mffas) {
        if (mffas.isInFreeFormMode()) {
            this.mController.mGestureListener.mFreeFormWindowMotionHelper.startGestureAnimation(25, mffas);
        } else {
            this.mController.mGestureListener.mFreeFormWindowMotionHelper.startGestureAnimation(30, mffas);
        }
    }

    private void startUnPinAnimation(MiuiFreeFormActivityStack mffas) {
        this.mController.mService.mAtmService.resumeAppSwitches();
        if (mffas != null && mffas.isInFreeFormMode()) {
            this.mController.mGestureListener.mFreeFormWindowMotionHelper.startGestureAnimation(28, mffas);
        } else if (mffas != null && mffas.isInMiniFreeFormMode()) {
            this.mController.mGestureListener.mFreeFormWindowMotionHelper.startGestureAnimation(29, mffas);
        }
    }

    private void setUpMiuiFreeWindowFloatIconAnimation(MiuiFreeFormActivityStack mffas, ActivityRecord activityRecord, DisplayContent displayContent, MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo) {
        int i = mffas.mLastFloatIconlayerTaskId;
        if (i != -1 && i != miuiFreeFormFloatIconInfo.getTaskId()) {
            Slog.i(TAG, "setUpMiuiFreeWindowFloatIconAnimation: no need to create icon layer for other task ActivityRecord");
        } else if (i != -1 && i == miuiFreeFormFloatIconInfo.getTaskId() && mffas.mLastTokenLayer >= activityRecord.getLastLayer()) {
            Slog.i(TAG, "setUpMiuiFreeWindowFloatIconAnimation: icon layer has created for the same task ActivityRecord");
        } else {
            clearMiuiFreeWindowFloatIconLayer(mffas);
            mffas.mLastFloatIconlayerTaskId = miuiFreeFormFloatIconInfo.getTaskId();
            mffas.mLastTokenLayer = activityRecord.getLastLayer();
            mffas.mLastIconLayerWindowToken = activityRecord;
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            createMiuiFreeWindowFloatIconSurfaceControl(mffas, activityRecord, displayContent, t);
            adjustMiuiFreeWindowFloatIconLayer(activityRecord, t);
            t.apply();
        }
    }

    private void createMiuiFreeWindowFloatIconSurfaceControl(MiuiFreeFormActivityStack mffas, ActivityRecord activityRecord, DisplayContent displayContent, SurfaceControl.Transaction transaction) {
        if (activityRecord.mFloatWindwoIconSurfaceControl != null) {
            return;
        }
        Rect rect = getRealTaskBounds(mffas);
        activityRecord.mFloatWindwoIconSurfaceControl = displayContent.makeOverlay().setName("MiuiFreeWindowFloatIconSurfaceControl").setBufferSize(rect.width(), rect.height()).setOpaque(false).build();
        Surface surface = (Surface) this.mController.mService.mSurfaceFactory.get();
        IMiuiScreenProjectionStub imp = IMiuiScreenProjectionStub.getInstance();
        if (this.mController.mService.isInScreenProjection()) {
            activityRecord.mFloatWindwoIconSurfaceControl.setScreenProjection(imp.getExtraScreenProjectFlag());
        }
        surface.copyFrom(activityRecord.mFloatWindwoIconSurfaceControl);
        try {
            Canvas canvas = surface.lockCanvas(null);
            if (canvas == null) {
                Slog.e(TAG, "createMiuiFreeWindowFloatIconSurfaceControl canvas is null");
            } else {
                int color = this.mController.mService.mContext.getColor(285605963);
                canvas.drawColor(color);
                surface.unlockCanvasAndPost(canvas);
                transaction.setPosition(activityRecord.mFloatWindwoIconSurfaceControl, EDGE_AREA, EDGE_AREA);
                transaction.setAlpha(activityRecord.mFloatWindwoIconSurfaceControl, EDGE_AREA);
                transaction.show(activityRecord.mFloatWindwoIconSurfaceControl);
                transaction.apply();
            }
        } catch (Surface.OutOfResourcesException e) {
            Slog.e(TAG, "createMiuiFreeWindowFloatIconSurfaceControl out of resource");
        } catch (IllegalArgumentException e2) {
            Slog.e(TAG, "createMiuiFreeWindowFloatIconSurfaceControl illegal argument");
        }
        surface.destroy();
    }

    private void adjustMiuiFreeWindowFloatIconLayer(ActivityRecord activityRecord, SurfaceControl.Transaction transaction) {
        if (activityRecord.getSurfaceControl() == null || !activityRecord.getSurfaceControl().isValid()) {
            Slog.i(TAG, "skip adjustMiuiFreeWindowFloatIconLayer: activityRecord.getSurfaceControl(): " + activityRecord.getSurfaceControl());
        } else if (activityRecord.mFloatWindwoIconSurfaceControl == null || !activityRecord.mFloatWindwoIconSurfaceControl.isValid()) {
            Slog.i(TAG, "skip adjustMiuiFreeWindowFloatIconLayer: activityRecord.mFloatWindwoIconSurfaceControl: " + activityRecord.mFloatWindwoIconSurfaceControl);
        } else {
            transaction.reparent(activityRecord.mFloatWindwoIconSurfaceControl, activityRecord.getSurfaceControl());
            transaction.setLayer(activityRecord.mFloatWindwoIconSurfaceControl, Integer.MAX_VALUE);
            transaction.apply(true);
            activityRecord.mHasIconLayer = true;
        }
    }

    private Rect getRealTaskBounds(MiuiFreeFormActivityStack mffas) {
        Rect rect = new Rect();
        if (mffas == null || mffas.mTask == null) {
            return rect;
        }
        rect.set(mffas.mTask.getBounds());
        int i = rect.left;
        int j = rect.top;
        rect.scale(mffas.mFreeFormScale);
        rect.offsetTo(i, j);
        return rect;
    }

    public void clearMiuiFreeWindowFloatIconLayer(MiuiFreeFormActivityStack mffas) {
        synchronized (this.mController.mService.mGlobalLock) {
            ActivityRecord lastIconLayerWindowToken = mffas.mLastIconLayerWindowToken;
            if (lastIconLayerWindowToken != null && lastIconLayerWindowToken.mHasIconLayer) {
                if (lastIconLayerWindowToken.mFloatWindwoIconSurfaceControl != null) {
                    if (lastIconLayerWindowToken.mFloatWindwoIconSurfaceControl.isValid()) {
                        SurfaceControl.Transaction transaction = (SurfaceControl.Transaction) this.mController.mService.mTransactionFactory.get();
                        transaction.remove(lastIconLayerWindowToken.mFloatWindwoIconSurfaceControl);
                        transaction.apply(true);
                        Slog.d(TAG, "clearMiuiFreeWindowFloatIconLayer" + lastIconLayerWindowToken.mFloatWindwoIconSurfaceControl);
                    }
                    lastIconLayerWindowToken.mFloatWindwoIconSurfaceControl = null;
                    lastIconLayerWindowToken.mHasIconLayer = false;
                    mffas.mLastFloatIconlayerTaskId = -1;
                }
            }
        }
    }

    private MiuiFreeFormFloatIconInfo getFloatIconInfo(MiuiFreeFormActivityStack mffas) {
        MiuiFreeFormFloatIconInfo defaultMiuiFreeFormFloatIconInfo = this.mMiuiFreeFormFloatIconInfos.get(Integer.valueOf(mffas.mTask.mTaskId));
        if (defaultMiuiFreeFormFloatIconInfo != null) {
            return defaultMiuiFreeFormFloatIconInfo;
        }
        MiuiFreeFormFloatIconInfo miuiFreeFormFloatIconInfo = new MiuiFreeFormFloatIconInfo();
        miuiFreeFormFloatIconInfo.setFloatPivotWidth(mffas.mExitIconWidth);
        miuiFreeFormFloatIconInfo.setFloatPivotHeight(mffas.mExitIconHeight);
        miuiFreeFormFloatIconInfo.setFloatPivotX(Float.valueOf(mffas.mExitPivotX));
        miuiFreeFormFloatIconInfo.setFloatPivotY(Float.valueOf(mffas.mExitPivotY));
        miuiFreeFormFloatIconInfo.setFloatVelocityX(Float.valueOf(mffas.mExitVelocityX));
        miuiFreeFormFloatIconInfo.setFloatVelocityY(Float.valueOf(mffas.mExitVelocityY));
        miuiFreeFormFloatIconInfo.setTaskId(mffas.mTask.mTaskId);
        miuiFreeFormFloatIconInfo.setIconBitmap(mffas.mExitIconBitmap);
        return miuiFreeFormFloatIconInfo;
    }

    public boolean hasMiniPined(List<MiuiFreeFormManager.MiuiFreeFormStackInfo> pinedList) {
        for (MiuiFreeFormManager.MiuiFreeFormStackInfo miuiFreeFormStackInfo : pinedList) {
            if (miuiFreeFormStackInfo.inPinMode && miuiFreeFormStackInfo.isInMiniFreeFormMode()) {
                return true;
            }
        }
        return false;
    }

    public void resetPinedCount() {
        setmPinedFreeformCount(0);
        setmPinedMiniFreeformCount(0);
    }

    public void calculatePinedCount(MiuiFreeFormActivityStack formActivityStack) {
        if (formActivityStack.isInFreeFormMode() && formActivityStack.inPinMode()) {
            setmPinedFreeformCount(getmPinedFreeformCount() + 1);
        }
        if (formActivityStack.isInMiniFreeFormMode() && formActivityStack.inPinMode()) {
            setmPinedMiniFreeformCount(getmPinedMiniFreeformCount() + 1);
        }
    }

    public int getmPinedFreeformCount() {
        return this.mPinedFreeformCount;
    }

    public int getmPinedMiniFreeformCount() {
        return this.mPinedMiniFreeformCount;
    }

    public void setmPinedFreeformCount(int pinedFreeformCount) {
        this.mPinedFreeformCount = pinedFreeformCount;
    }

    public void setmPinedMiniFreeformCount(int pinedMiniFreeformCount) {
        this.mPinedMiniFreeformCount = pinedMiniFreeformCount;
    }
}
