package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.DisplayMetrics;
import android.util.MiuiMultiWindowUtils;
import android.util.RotationUtils;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.MiuiFreeFormActivityStack;
import com.android.server.wm.MiuiFreeFormGestureAnimator;
import com.android.server.wm.MiuiFreeFormWindowMotionHelper;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.android.server.wm.utils.RotationAnimationUtils;
import com.google.android.collect.Sets;
import com.xiaomi.screenprojection.IMiuiScreenProjectionStub;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiFreeFormWindowMotionHelper {
    private static final float CLOSE_OPERATION_THRESHOLD_QUICK = 80.0f;
    private static final float CLOSE_OPERATION_THRESHOLD_VELOCITY_Y = -1000.0f;
    private static final int CYCLE_WAITING_MS = 100;
    private static final float LANDSCAPE_OPEN_OPERATION_THRESHOLD;
    private static final int MAX_WAITING_RESIZE_COMPLE_TIME = 100;
    private static final int OFFSET = 20;
    private static final float OPEN_OPERATION_THRESHOLD = 300.0f;
    private static final int RESIZE_BACK_TO_MAXSIZE = 0;
    private static final int RESIZE_BACK_TO_MINSIZE = 1;
    private static final int RESIZE_BACK_UNDEFINED = -1;
    private static final String TAG = "MiuiFreeFormWindowMotionHelper";
    private static final int TOUCH_BOTTOM = 1;
    private static final int TOUCH_RESIZE_LEFT = 2;
    private static final int TOUCH_RESIZE_RIGHT = 3;
    public static final int TOUCH_TOP = 0;
    private static final int TOUCH_UNDEFINED = -1;
    private static final float WINDOW_NEED_OFFSETY = 200.0f;
    private static final Object mLock = new Object();
    private volatile boolean mAlreadySetXScale;
    private volatile boolean mAlreadySetYScale;
    private long mAnimationStartTime;
    int mCurrentTouchedMode;
    private float mCurrentX;
    private float mCurrentY;
    private SurfaceControl mDimmerLayer;
    private volatile float mDownX;
    private volatile float mDownY;
    private boolean mInResizeMaxSize;
    private boolean mInResizeMinSize;
    private volatile float mLastMoveX;
    private volatile float mLastMoveY;
    private MiuiFreeFormGesturePointerEventListener mListener;
    private MiuiFreeFormTaskPositioner mPositioner;
    private float mProvitXOffset;
    private float mResizeCurrentScale;
    private float mResizeLeftPosition;
    private float mResizeMaxX;
    private float mResizeMinX;
    private float mResizeScale;
    private long mResizeStartTime;
    private float mResizeTopPosition;
    private Animation mScaleAnimator;
    private SurfaceControl mSurfaceControl;
    private Rect mBottomGestureDetectBounds = new Rect();
    private Rect mTopGestureDetectBounds = new Rect();
    private Rect mLeftResizeGestureDetectBounds = new Rect();
    private Rect mRightResizeGestureDetectBounds = new Rect();
    private Rect mOriginalBounds = new Rect();
    ValueAnimator mShadowAnimator = new ValueAnimator();
    private boolean mHadVibrator = false;
    AtomicInteger mLandDropAnimation = new AtomicInteger(-1);
    private final Interpolator mDecelerateInterpolator = new DecelerateInterpolator(1.5f);
    private AnimatorSet mGestureAnimatorSet = new AnimatorSet();
    final ConcurrentHashMap<MiuiFreeFormActivityStack, MiuiFreeFormGestureAnimator.AnimalLock> mStackLocks = new ConcurrentHashMap<>();
    boolean mShowedHotSpotView = false;
    public boolean mEnteredHotArea = false;
    private boolean mEnteredRemanderBottomBarHotSpotView = false;
    private boolean mEnteredTriggerBottomBarHotArea = false;
    private int lastActivedBottomBarHotSpot = -1;
    private Rect mHotSpotWindowBounds = new Rect();
    private int mHotSpotNum = -1;
    private int mBottomBarHotSpotNum = -1;
    private float mResizeLeft = -1.0f;
    private float mResizeTop = -1.0f;
    private int mResizeLastMoveLeft = -1;
    private int mResizeLastMoveTop = -1;
    private int mResizeLastMoveRight = -1;
    private int mResizeLastMoveBottom = -1;
    private int mResizeBackMode = -1;
    private int mDownNumbers = 0;
    private long mFirstDownTime = 0;
    private final int SCREEN_FREEZE_LAYER_BASE = 2010000;
    private final int SCREEN_FREEZE_LAYER_SCREENSHOT = 2009999;
    private VelocityMonitor mVelocityMonitor = new VelocityMonitor();
    private Object mSurfaceControlLock = new Object();
    private final Paint mPaint = new Paint(1);
    private final Matrix mTempMatrix = new Matrix();

    static {
        LANDSCAPE_OPEN_OPERATION_THRESHOLD = Build.IS_TABLET ? 100.0f : 25.0f;
    }

    public MiuiFreeFormWindowMotionHelper(MiuiFreeFormGesturePointerEventListener listener) {
        this.mListener = listener;
    }

    public boolean isInFreeFormControlRegon(float x, float y) {
        MiuiFreeFormActivityStack mffs = this.mListener.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
        return isInFreeFormControlRegon(x, y, mffs);
    }

    public boolean isInFreeFormResizeControlRegon(float x, float y, MiuiFreeFormActivityStack mffs) {
        if (mffs == null) {
            return false;
        }
        Rect windowBounds = new Rect(mffs.mTask.getBounds());
        float mWindowHeight = windowBounds.height() * mffs.mFreeFormScale;
        float width = windowBounds.width() * mffs.mFreeFormScale;
        int hotSpaceBottomHeight = MiuiMultiWindowUtils.multiFreeFormSupported(this.mListener.mService.mContext) ? 45 : 54;
        int mLeftResizeDetecteLeftPoint = windowBounds.left;
        int mLeftResizeDetecteTopPoint = (int) ((windowBounds.top + mWindowHeight) - hotSpaceBottomHeight);
        int mLeftResizeDetecteRightPoint = MiuiMultiWindowUtils.RESIZE_WIDTH + mLeftResizeDetecteLeftPoint;
        int mLeftResizeDetecteBottomPoint = ((int) (windowBounds.top + mWindowHeight)) + MiuiMultiWindowUtils.HOT_SPACE_OFFSITE;
        int mBottomDetecteRightPoint = (int) (((windowBounds.left + mffs.mStackControlInfo.mScaleWindowWidth) - MiuiMultiWindowUtils.RESIZE_WIDTH) - 1.0f);
        int mRightResizeDetecteLeftPoint = mBottomDetecteRightPoint + 1;
        int mRightResizeDetecteRightPoint = MiuiMultiWindowUtils.RESIZE_WIDTH + mRightResizeDetecteLeftPoint;
        int mRightResizeDetecteTopPoint = (int) ((windowBounds.top + mWindowHeight) - hotSpaceBottomHeight);
        int mRightResizeDetecteBottomPoint = ((int) (windowBounds.top + mWindowHeight)) + MiuiMultiWindowUtils.HOT_SPACE_OFFSITE;
        Rect leftResizeGestureDetectBounds = new Rect();
        Rect rightResizeGestureDetectBounds = new Rect();
        leftResizeGestureDetectBounds.set(mLeftResizeDetecteLeftPoint, mLeftResizeDetecteTopPoint, mLeftResizeDetecteRightPoint, mLeftResizeDetecteBottomPoint);
        rightResizeGestureDetectBounds.set(mRightResizeDetecteLeftPoint, mRightResizeDetecteTopPoint, mRightResizeDetecteRightPoint, mRightResizeDetecteBottomPoint);
        int mRightResizeDetecteTopPoint2 = (int) x;
        int mRightResizeDetecteBottomPoint2 = (int) y;
        boolean isIn = leftResizeGestureDetectBounds.contains(mRightResizeDetecteTopPoint2, mRightResizeDetecteBottomPoint2) || rightResizeGestureDetectBounds.contains((int) x, (int) y);
        Slog.d(TAG, "DEBUG_CONTROL isInFreeFormResizeControlRegon isIn = : " + isIn + " (x,y) = (" + x + "," + y + ")");
        return isIn;
    }

    public boolean isInFreeFormControlRegon(float x, float y, MiuiFreeFormActivityStack mffs) {
        Rect freeFormVisualRect;
        boolean isInResizeGestureDetectRegion;
        if (mffs == null) {
            return false;
        }
        int screenType = MiuiMultiWindowUtils.getScreenType(this.mListener.mService.mContext);
        Slog.d(TAG, "DEBUG_CONTROL isInFreeFormControlRegon = : " + mffs.getStackPackageName());
        if (mffs.isInMiniFreeFormMode()) {
            boolean isIn = mffs.mStackControlInfo.mSmallWindowBounds.contains((int) x, (int) y);
            if (screenType == 3) {
                MiuiMultiWindowUtils.RoundedRectF leftResizeRegion = MiuiMultiWindowUtils.getCvwResizeRegion(4, mffs.mStackControlInfo.mSmallWindowBounds);
                MiuiMultiWindowUtils.RoundedRectF rightResizeRegion = MiuiMultiWindowUtils.getCvwResizeRegion(5, mffs.mStackControlInfo.mSmallWindowBounds);
                if (leftResizeRegion.contains(x, y) || rightResizeRegion.contains(x, y)) {
                    return false;
                }
            }
            Slog.d(TAG, "DEBUG_CONTROL isInFreeFormControlRegon isIn = : " + isIn + " (x,y) = (" + x + "," + y + ")  mffs.mStackControlInfo.mSmallWindowBounds = " + mffs.mStackControlInfo.mSmallWindowBounds);
            return isIn;
        }
        synchronized (this.mListener.mService.mGlobalLock) {
            WindowState imeWin = this.mListener.mService.mRoot.getCurrentInputMethodWindow();
            ActivityRecord topFullScreenActivity = mffs.mTask.getTopFullscreenActivity();
            WindowState mainWindow = topFullScreenActivity != null ? topFullScreenActivity.findMainWindow() : null;
            if (imeWin != null && imeWin.isVisibleNow() && mainWindow != null) {
                freeFormVisualRect = MiuiMultiWindowUtils.getVisualBounds(mainWindow.getFrame(), mffs.mFreeFormScale);
            } else {
                freeFormVisualRect = MiuiMultiWindowUtils.getVisualBounds(mffs.mTask.getBounds(), mffs.mFreeFormScale);
            }
        }
        Rect topGestureDetectBounds = MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 0, freeFormVisualRect, this.mListener.mService.mContext);
        Rect bottomGestureDetectBounds = MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 1, freeFormVisualRect, this.mListener.mService.mContext);
        boolean isInTopBottomGestureDetectRegion = topGestureDetectBounds.contains((int) x, (int) y) || bottomGestureDetectBounds.contains((int) x, (int) y);
        if (screenType == 3) {
            MiuiMultiWindowUtils.RoundedRectF leftResizeRegion2 = MiuiMultiWindowUtils.getCvwResizeRegion(4, freeFormVisualRect);
            MiuiMultiWindowUtils.RoundedRectF rightResizeRegion2 = MiuiMultiWindowUtils.getCvwResizeRegion(5, freeFormVisualRect);
            isInResizeGestureDetectRegion = leftResizeRegion2.contains(x, y) || rightResizeRegion2.contains(x, y);
        } else {
            Rect leftResizeGestureDetectBounds = MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 2, freeFormVisualRect, this.mListener.mService.mContext);
            Rect rightResizeGestureDetectBounds = MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 3, freeFormVisualRect, this.mListener.mService.mContext);
            isInResizeGestureDetectRegion = leftResizeGestureDetectBounds.contains((int) x, (int) y) || rightResizeGestureDetectBounds.contains((int) x, (int) y);
        }
        Slog.d(TAG, "DEBUG_CONTROL isInFreeFormControlRegon isInTopBottomGestureDetectRegion: " + isInTopBottomGestureDetectRegion + " isInResizeGestureDetectRegion: " + isInResizeGestureDetectRegion + " (x,y) = (" + x + "," + y + ") topGestureDetectBounds = " + topGestureDetectBounds);
        return isInTopBottomGestureDetectRegion || isInResizeGestureDetectRegion;
    }

    public MiuiFreeFormActivityStack findControlFreeFormActivityStack(final float x, final float y) {
        final MiuiFreeFormManagerService service = this.mListener.mGestureController.mMiuiFreeFormManagerService;
        TaskDisplayArea defaultTaskDisplayArea = service.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "DEBUG_CONTROL DisplayArea.getTaskSTAT= : ");
        }
        synchronized (this.mListener.mService.mGlobalLock) {
            Task topTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormWindowMotionHelper.this.m1747x7f73bb81(service, x, y, (Task) obj);
                }
            }, true);
            if (topTask == null && (topTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormWindowMotionHelper.this.m1748x998f3a20(service, x, y, (Task) obj);
                }
            }, true)) == null) {
                topTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$$ExternalSyntheticLambda2
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return MiuiFreeFormWindowMotionHelper.this.m1749xb3aab8bf(service, x, y, (Task) obj);
                    }
                }, true);
            }
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "DEBUG_CONTROL DisplayArea.getTaskEND= : ");
            }
            if (topTask == null) {
                return null;
            }
            MiuiFreeFormActivityStack topFfas = service.getMiuiFreeFormActivityStackForMiuiFB(topTask.getRootTaskId());
            if (topFfas != null && topFfas.isInMiniFreeFormMode()) {
                return topFfas;
            }
            final Rect tempWindowBound = new Rect();
            Task pointInTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$$ExternalSyntheticLambda3
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormWindowMotionHelper.lambda$findControlFreeFormActivityStack$3(MiuiFreeFormManagerService.this, x, y, tempWindowBound, (Task) obj);
                }
            }, true);
            if ((pointInTask != null && topTask.getRootTaskId() != pointInTask.getRootTaskId()) || (MiuiCvwGestureController.isMiuiCvwFeatureEnable() && MiuiMultiWindowUtils.inCvwResizeRegion(MiuiMultiWindowUtils.getVisualBounds(topFfas.mTask.getBounds(), topFfas.mFreeFormScale), x, y))) {
                return null;
            }
            return topFfas;
        }
    }

    /* renamed from: lambda$findControlFreeFormActivityStack$0$com-android-server-wm-MiuiFreeFormWindowMotionHelper */
    public /* synthetic */ boolean m1747x7f73bb81(MiuiFreeFormManagerService service, float x, float y, Task t) {
        if (t.inFreeformSmallWinMode()) {
            MiuiFreeFormActivityStack mffs = service.getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId());
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "DEBUG_CONTROL DisplayArea.getTaskIN: " + t);
            }
            if (mffs != null && mffs.isInMiniFreeFormMode() && isInFreeFormControlRegon(x, y, mffs) && !mffs.inPinMode()) {
                if (MiuiFreeFormGestureController.DEBUG) {
                    Slog.d(TAG, "DEBUG_CONTROL findControlFreeFormActivityStack = : " + mffs.getStackPackageName());
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /* renamed from: lambda$findControlFreeFormActivityStack$1$com-android-server-wm-MiuiFreeFormWindowMotionHelper */
    public /* synthetic */ boolean m1748x998f3a20(MiuiFreeFormManagerService service, float x, float y, Task t) {
        if (t.inFreeformSmallWinMode()) {
            MiuiFreeFormActivityStack mffs = service.getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId());
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "DEBUG_CONTROL DisplayArea.getTaskIN: " + t);
            }
            if (isInFreeFormControlRegon(x, y, mffs) && !mffs.inPinMode()) {
                if (MiuiFreeFormGestureController.DEBUG) {
                    Slog.d(TAG, "DEBUG_CONTROL findControlFreeFormActivityStack = : " + mffs.getStackPackageName());
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    /* renamed from: lambda$findControlFreeFormActivityStack$2$com-android-server-wm-MiuiFreeFormWindowMotionHelper */
    public /* synthetic */ boolean m1749xb3aab8bf(MiuiFreeFormManagerService service, float x, float y, Task t) {
        if (t.inFreeformSmallWinMode()) {
            MiuiFreeFormActivityStack mffs = service.getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId());
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "DEBUG_CONTROL DisplayArea.getTaskIN: " + t);
            }
            if (isInFreeFormControlRegon(x, y, mffs) && !mffs.inPinMode()) {
                if (MiuiFreeFormGestureController.DEBUG) {
                    Slog.d(TAG, "DEBUG_CONTROL findControlFreeFormActivityStack = : " + mffs.getStackPackageName());
                    return true;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public static /* synthetic */ boolean lambda$findControlFreeFormActivityStack$3(MiuiFreeFormManagerService service, float x, float y, Rect tempWindowBound, Task t) {
        MiuiFreeFormActivityStack mffs;
        if (t.inFreeformSmallWinMode() && (mffs = service.getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId())) != null) {
            if (mffs.isInMiniFreeFormMode()) {
                return mffs.mStackControlInfo.mSmallWindowBounds.contains((int) x, (int) y);
            }
            Rect windowBounds = new Rect(mffs.mTask.getBounds());
            float mWindowHeight = windowBounds.height() * mffs.mFreeFormScale;
            float mWindowWidth = windowBounds.width() * mffs.mFreeFormScale;
            tempWindowBound.set(windowBounds.left, windowBounds.top, windowBounds.left + ((int) mWindowWidth), windowBounds.top + ((int) mWindowHeight));
            if (tempWindowBound.contains((int) x, (int) y)) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean notifyDownLocked(MotionEvent motionEvent, MiuiFreeFormActivityStack stack) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null || animalLock.mCurrentAnimation == -1) {
            this.mDownX = motionEvent.getX();
            this.mDownY = motionEvent.getY();
            if (this.mListener.isInExcludeRegion(this.mDownX, this.mDownY)) {
                stack.mStackBeenHandled = false;
                Slog.d(TAG, "should intercept touch event");
                return false;
            }
            DisplayContent displayContent = this.mListener.mGestureController.mDisplayContent;
            this.mListener.updateScreenParams(displayContent, displayContent.getConfiguration());
            stack.mStackControlInfo.mCurrentAction = 0;
            stack.mStackControlInfo.mScaleWindowHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
            stack.mStackControlInfo.mScaleWindowWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            initGestureDetecteSpace(stack);
            stack.mStackControlInfo.mLastFreeFormWindowStartBounds = new Rect(stack.mTask.getBounds());
            this.mResizeBackMode = -1;
            stack.mStackControlInfo.mCurrentGestureAction = -1;
            stack.mStackControlInfo.mCurrentAnimation = -1;
            this.mEnteredHotArea = false;
            this.mCurrentX = this.mDownX;
            this.mCurrentY = this.mDownY;
            this.mLastMoveX = this.mDownX;
            this.mLastMoveY = this.mDownY;
            stack.mStackBeenHandled = true;
            this.mHadVibrator = false;
            stack.mStackControlInfo.mNowPosX = stack.mTask.getBounds().left;
            stack.mStackControlInfo.mNowPosY = stack.mTask.getBounds().top;
            stack.mStackControlInfo.mNowWidthScale = 1.0f;
            stack.mStackControlInfo.mNowHeightScale = 1.0f;
            stack.mStackControlInfo.mNowAlpha = 1.0f;
            boolean isMiuiCvwFeatureEnable = MiuiCvwGestureController.isMiuiCvwFeatureEnable();
            this.mVelocityMonitor.clear();
            this.mVelocityMonitor.update(motionEvent.getRawX(), motionEvent.getRawY());
            if (!this.mTopGestureDetectBounds.contains((int) this.mDownX, (int) this.mDownY)) {
                if (this.mBottomGestureDetectBounds.contains((int) this.mDownX, (int) this.mDownY)) {
                    synchronized (this.mListener.mService.mGlobalLock) {
                        if (stack.getFreeFormConrolSurface() == null) {
                            this.mListener.mGestureAnimator.createLeash(stack);
                        }
                    }
                    this.mCurrentTouchedMode = 1;
                    this.mProvitXOffset = this.mCurrentX - stack.mTask.getBounds().left;
                    this.mListener.mGestureController.mWindowController.showBottomBarHotSpotView();
                } else if (!isMiuiCvwFeatureEnable && this.mLeftResizeGestureDetectBounds.contains((int) this.mDownX, (int) this.mDownY)) {
                    this.mListener.mGestureController.notifyFreeFormApplicationResizeStart(stack);
                    this.mResizeStartTime = SystemClock.uptimeMillis();
                    synchronized (this.mListener.mService.mGlobalLock) {
                        if (stack.getFreeFormConrolSurface() == null) {
                            this.mListener.mGestureAnimator.createLeash(stack);
                        }
                    }
                    this.mResizeLeftPosition = stack.mTask.getBounds().left;
                    this.mResizeTopPosition = stack.mTask.getBounds().top;
                    this.mResizeLastMoveLeft = stack.mTask.getBounds().left;
                    this.mResizeLastMoveTop = stack.mTask.getBounds().top;
                    this.mResizeLastMoveRight = this.mResizeLastMoveLeft + ((int) stack.mStackControlInfo.mScaleWindowWidth);
                    this.mResizeLastMoveBottom = this.mResizeLastMoveTop + ((int) stack.mStackControlInfo.mScaleWindowHeight);
                    this.mInResizeMinSize = false;
                    this.mInResizeMaxSize = false;
                    this.mResizeScale = stack.mFreeFormScale;
                    this.mResizeLeft = this.mResizeLastMoveLeft;
                    this.mResizeTop = this.mResizeLastMoveTop;
                    this.mCurrentTouchedMode = 2;
                    Slog.d(TAG, "mCurrentTouchedMode = TOUCH_RESIZE_LEFT");
                } else if (!isMiuiCvwFeatureEnable && this.mRightResizeGestureDetectBounds.contains((int) this.mDownX, (int) this.mDownY)) {
                    this.mListener.mGestureController.notifyFreeFormApplicationResizeStart(stack);
                    this.mResizeStartTime = SystemClock.uptimeMillis();
                    synchronized (this.mListener.mService.mGlobalLock) {
                        if (stack.getFreeFormConrolSurface() == null) {
                            this.mListener.mGestureAnimator.createLeash(stack);
                        }
                    }
                    this.mResizeLastMoveLeft = stack.mTask.getBounds().left;
                    this.mResizeLastMoveTop = stack.mTask.getBounds().top;
                    this.mResizeLastMoveRight = this.mResizeLastMoveLeft + ((int) stack.mStackControlInfo.mScaleWindowWidth);
                    this.mResizeLastMoveBottom = this.mResizeLastMoveTop + ((int) stack.mStackControlInfo.mScaleWindowHeight);
                    this.mInResizeMinSize = false;
                    this.mInResizeMaxSize = false;
                    this.mResizeScale = stack.mFreeFormScale;
                    this.mResizeLeft = this.mResizeLastMoveLeft;
                    this.mResizeTop = this.mResizeLastMoveTop;
                    this.mCurrentTouchedMode = 3;
                    Slog.d(TAG, "mCurrentTouchedMode = TOUCH_RESIZE_RIGHT");
                } else {
                    this.mCurrentTouchedMode = -1;
                    stack.mStackBeenHandled = false;
                }
            } else {
                synchronized (this.mListener.mService.mGlobalLock) {
                    if (stack.getFreeFormConrolSurface() == null) {
                        this.mListener.mGestureAnimator.createLeash(stack);
                    }
                }
                this.mCurrentTouchedMode = 0;
                this.mListener.mTaskPositioner.updateWindowDownBounds(this.mCurrentX, this.mCurrentY, this.mDownX, this.mDownY, 0.0f, 0.0f, this.mListener.mIsPortrait, stack.mTask.getBounds(), stack);
            }
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "mStackBeenHandled:" + stack.mStackBeenHandled + " mDownX:" + this.mDownX + " mDownY:" + this.mDownY);
            }
            return true;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:33:0x010a  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x01af  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x01c9  */
    /* JADX WARN: Removed duplicated region for block: B:51:0x01e0  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean notifyMoveLocked(android.view.MotionEvent r21, com.android.server.wm.MiuiFreeFormActivityStack r22) {
        /*
            Method dump skipped, instructions count: 1742
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.notifyMoveLocked(android.view.MotionEvent, com.android.server.wm.MiuiFreeFormActivityStack):boolean");
    }

    private void leftBottom(float dx, float x, float y, MiuiFreeFormActivityStack stack, boolean isPortrait) {
        int bottomMargin;
        int oriLeft = this.mResizeLastMoveLeft;
        int oriTop = this.mResizeLastMoveTop;
        int oriRight = this.mResizeLastMoveRight;
        int i = this.mResizeLastMoveBottom;
        float oriFreeformLandscapeHeight = getOriFreeformLandscapeHeight(stack);
        if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext) || MiuiMultiWindowUtils.isFoldInnerScreen(this.mListener.mService.mContext)) {
            bottomMargin = 0;
        } else {
            bottomMargin = (this.mListener.mScreenHeight - ((int) (oriFreeformLandscapeHeight + 0.5f))) / 2;
        }
        int oriLeft2 = (int) (oriLeft + dx);
        int oriBottom = ((int) (((oriRight - oriLeft2) / stack.mWidthHeightScale) + 0.5f)) + oriTop;
        if (oriLeft2 < this.mListener.mNotchBar && !isPortrait && this.mListener.mDisplayRotation == 1) {
            oriLeft2 = this.mListener.mNotchBar;
            oriBottom = oriTop + ((int) (((oriRight - oriLeft2) / stack.mWidthHeightScale) + 0.5f));
        } else if (stack.mIsLandcapeFreeform && !isPortrait && oriBottom > this.mListener.mScreenHeight - bottomMargin) {
            oriBottom = this.mListener.mScreenHeight - bottomMargin;
            oriLeft2 = oriRight - ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (oriBottom > this.mListener.mScreenHeight) {
            oriBottom = this.mListener.mScreenHeight;
            oriLeft2 = oriRight - ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (oriLeft2 < 0) {
            oriLeft2 = 0;
            oriBottom = oriTop + ((int) (((oriRight - 0) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriBottom > this.mListener.mFreeFormAccessibleArea.bottom) {
            oriBottom = this.mListener.mFreeFormAccessibleArea.bottom;
            oriLeft2 = oriRight - ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        }
        if (oriLeft2 < this.mListener.mFreeFormAccessibleArea.left && this.mListener.mDisplayRotation == 3) {
            oriLeft2 = this.mListener.mFreeFormAccessibleArea.left;
            oriBottom = oriTop + ((int) (((oriRight - oriLeft2) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight - oriLeft2 < stack.mMaxMinWidthSize[1]) {
            if (!this.mInResizeMinSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMinSize = true;
                this.mResizeMinX = x;
            }
            int adjustedWidth = stack.mMaxMinWidthSize[1] - ((int) afterFriction(Math.abs(x - this.mResizeMinX), stack.mMaxMinWidthSize[1]));
            oriLeft2 = oriRight - adjustedWidth;
            oriBottom = oriTop + ((int) ((adjustedWidth / stack.mWidthHeightScale) + 0.5f));
        } else if (oriRight - oriLeft2 > stack.mMaxMinWidthSize[0]) {
            if (!this.mInResizeMaxSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMaxSize = true;
                this.mResizeMaxX = x;
            }
            int adjustedWidth2 = stack.mMaxMinWidthSize[0] + ((int) afterFriction(Math.abs(x - this.mResizeMaxX), stack.mMaxMinWidthSize[0]));
            oriLeft2 = oriRight - adjustedWidth2;
            oriBottom = oriTop + ((int) ((adjustedWidth2 / stack.mWidthHeightScale) + 0.5f));
        } else {
            this.mInResizeMinSize = false;
            this.mInResizeMaxSize = false;
        }
        this.mResizeLastMoveLeft = oriLeft2;
        this.mResizeLastMoveTop = oriTop;
        this.mResizeLastMoveRight = oriRight;
        this.mResizeLastMoveBottom = oriBottom;
        Slog.i(TAG, "leftBottom");
        setLeashPositionAndScale(new Rect(oriLeft2, oriTop, oriRight, oriBottom), stack);
    }

    private void bottomLeft(float dy, float x, float y, MiuiFreeFormActivityStack stack, boolean isPortrait) {
        int bottomMargin;
        int i = this.mResizeLastMoveLeft;
        int oriTop = this.mResizeLastMoveTop;
        int oriRight = this.mResizeLastMoveRight;
        int oriBottom = this.mResizeLastMoveBottom;
        float oriFreeformLandscapeHeight = getOriFreeformLandscapeHeight(stack);
        if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext) || MiuiMultiWindowUtils.isFoldInnerScreen(this.mListener.mService.mContext)) {
            bottomMargin = 0;
        } else {
            bottomMargin = (this.mListener.mScreenHeight - ((int) (oriFreeformLandscapeHeight + 0.5f))) / 2;
        }
        int oriBottom2 = (int) (oriBottom + dy);
        int oriLeft = oriRight - ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        if (oriLeft < this.mListener.mNotchBar && !isPortrait && this.mListener.mDisplayRotation == 1) {
            oriLeft = this.mListener.mNotchBar;
            oriBottom2 = oriTop + ((int) (((oriRight - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        } else if (!stack.mIsLandcapeFreeform && oriBottom2 > this.mListener.mScreenHeight) {
            oriBottom2 = this.mListener.mScreenHeight;
            oriLeft = oriRight - ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (stack.mIsLandcapeFreeform && !isPortrait && oriBottom2 > this.mListener.mScreenHeight - bottomMargin) {
            oriBottom2 = this.mListener.mScreenHeight - bottomMargin;
            oriLeft = oriRight - ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (oriLeft < 0) {
            oriLeft = 0;
            oriBottom2 = oriTop + ((int) (((oriRight - 0) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriBottom2 > this.mListener.mFreeFormAccessibleArea.bottom) {
            oriBottom2 = this.mListener.mFreeFormAccessibleArea.bottom;
            oriLeft = oriRight - ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        }
        if (oriLeft < this.mListener.mFreeFormAccessibleArea.left && this.mListener.mDisplayRotation == 3) {
            oriLeft = this.mListener.mFreeFormAccessibleArea.left;
            oriBottom2 = oriTop + ((int) (((oriRight - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight - oriLeft < stack.mMaxMinWidthSize[1]) {
            if (!this.mInResizeMinSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMinSize = true;
                this.mResizeMinX = x;
            }
            int adjustedWidth = stack.mMaxMinWidthSize[1] - ((int) afterFriction(Math.abs(x - this.mResizeMinX), stack.mMaxMinWidthSize[1]));
            oriLeft = oriRight - adjustedWidth;
            oriBottom2 = oriTop + ((int) ((adjustedWidth / stack.mWidthHeightScale) + 0.5f));
        } else if (oriRight - oriLeft > stack.mMaxMinWidthSize[0]) {
            if (!this.mInResizeMaxSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMaxSize = true;
                this.mResizeMaxX = x;
            }
            int adjustedWidth2 = stack.mMaxMinWidthSize[0] + ((int) afterFriction(Math.abs(x - this.mResizeMaxX), stack.mMaxMinWidthSize[0]));
            oriLeft = oriRight - adjustedWidth2;
            oriBottom2 = oriTop + ((int) ((adjustedWidth2 / stack.mWidthHeightScale) + 0.5f));
        } else {
            this.mInResizeMinSize = false;
            this.mInResizeMaxSize = false;
        }
        this.mResizeLastMoveLeft = oriLeft;
        this.mResizeLastMoveTop = oriTop;
        this.mResizeLastMoveRight = oriRight;
        this.mResizeLastMoveBottom = oriBottom2;
        Slog.i(TAG, "bottomLeft");
        setLeashPositionAndScale(new Rect(oriLeft, oriTop, oriRight, oriBottom2), stack);
    }

    private void rightBottom(float dx, float x, float y, MiuiFreeFormActivityStack stack) {
        int bottomMargin;
        int oriLeft = this.mResizeLastMoveLeft;
        int oriTop = this.mResizeLastMoveTop;
        int oriRight = this.mResizeLastMoveRight;
        int i = this.mResizeLastMoveBottom;
        float oriFreeformLandscapeHeight = getOriFreeformLandscapeHeight(stack);
        if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext) || MiuiMultiWindowUtils.isFoldInnerScreen(this.mListener.mService.mContext)) {
            bottomMargin = 0;
        } else {
            bottomMargin = (this.mListener.mScreenHeight - ((int) (oriFreeformLandscapeHeight + 0.5f))) / 2;
        }
        int oriRight2 = (int) (oriRight + dx);
        int oriBottom = ((int) (((oriRight2 - oriLeft) / stack.mWidthHeightScale) + 0.5f)) + oriTop;
        if (oriRight2 > this.mListener.mScreenWidth - this.mListener.mNotchBar && !this.mListener.mIsPortrait && this.mListener.mDisplayRotation == 3) {
            oriRight2 = this.mListener.mScreenWidth - this.mListener.mNotchBar;
            oriBottom = oriTop + ((int) (((oriRight2 - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        } else if (oriRight2 > this.mListener.mScreenWidth) {
            oriRight2 = this.mListener.mScreenWidth;
            oriBottom = oriTop + ((int) (((oriRight2 - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        } else if (stack.mIsLandcapeFreeform && !this.mListener.mIsPortrait && oriBottom > this.mListener.mScreenHeight - bottomMargin) {
            oriBottom = this.mListener.mScreenHeight - bottomMargin;
            oriRight2 = oriLeft + ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (oriBottom > this.mListener.mScreenHeight) {
            oriBottom = this.mListener.mScreenHeight;
            oriRight2 = oriLeft + ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        }
        if (oriBottom > this.mListener.mFreeFormAccessibleArea.bottom) {
            oriBottom = this.mListener.mFreeFormAccessibleArea.bottom;
            oriRight2 = oriLeft + ((int) (((oriBottom - oriTop) * stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight2 > this.mListener.mFreeFormAccessibleArea.right && this.mListener.mDisplayRotation == 1) {
            oriRight2 = this.mListener.mFreeFormAccessibleArea.right;
            oriBottom = oriTop + ((int) (((oriRight2 - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight2 - oriLeft < stack.mMaxMinWidthSize[1]) {
            if (!this.mInResizeMinSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMinSize = true;
                this.mResizeMinX = x;
            }
            int adjustedWidth = stack.mMaxMinWidthSize[1] - ((int) afterFriction(Math.abs(x - this.mResizeMinX), stack.mMaxMinWidthSize[1]));
            oriRight2 = oriLeft + adjustedWidth;
            oriBottom = oriTop + ((int) ((adjustedWidth / stack.mWidthHeightScale) + 0.5f));
        } else if (oriRight2 - oriLeft > stack.mMaxMinWidthSize[0]) {
            if (!this.mInResizeMaxSize) {
                this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                this.mInResizeMaxSize = true;
                this.mResizeMaxX = x;
            }
            int adjustedWidth2 = stack.mMaxMinWidthSize[0] + ((int) afterFriction(Math.abs(x - this.mResizeMaxX), stack.mMaxMinWidthSize[0]));
            oriRight2 = oriLeft + adjustedWidth2;
            oriBottom = oriTop + ((int) ((adjustedWidth2 / stack.mWidthHeightScale) + 0.5f));
        } else {
            this.mInResizeMinSize = false;
            this.mInResizeMaxSize = false;
        }
        this.mResizeLastMoveLeft = oriLeft;
        this.mResizeLastMoveTop = oriTop;
        this.mResizeLastMoveRight = oriRight2;
        this.mResizeLastMoveBottom = oriBottom;
        Slog.i(TAG, "rightBottom");
        setLeashPositionAndScale(new Rect(oriLeft, oriTop, oriRight2, oriBottom), stack);
    }

    private void bottomRight(float dy, float x, float y, MiuiFreeFormActivityStack stack) {
        int bottomMargin;
        int oriLeft = this.mResizeLastMoveLeft;
        int oriTop = this.mResizeLastMoveTop;
        int i = this.mResizeLastMoveRight;
        int oriBottom = this.mResizeLastMoveBottom;
        float oriFreeformLandscapeHeight = getOriFreeformLandscapeHeight(stack);
        if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext) || MiuiMultiWindowUtils.isFoldInnerScreen(this.mListener.mService.mContext)) {
            bottomMargin = 0;
        } else {
            bottomMargin = (this.mListener.mScreenHeight - ((int) (oriFreeformLandscapeHeight + 0.5f))) / 2;
        }
        int oriBottom2 = (int) (oriBottom + dy);
        int oriRight = ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f)) + oriLeft;
        if (oriRight > this.mListener.mScreenWidth - this.mListener.mNotchBar && !this.mListener.mIsPortrait && this.mListener.mDisplayRotation == 3) {
            oriRight = this.mListener.mScreenWidth - this.mListener.mNotchBar;
            oriBottom2 = oriTop + ((int) (((oriRight - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        } else if (!stack.mIsLandcapeFreeform && oriBottom2 > this.mListener.mScreenHeight) {
            oriBottom2 = this.mListener.mScreenHeight;
            oriRight = oriLeft + ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (stack.mIsLandcapeFreeform && !this.mListener.mIsPortrait && oriBottom2 > this.mListener.mScreenHeight - bottomMargin) {
            oriBottom2 = this.mListener.mScreenHeight - bottomMargin;
            oriRight = oriLeft + ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        } else if (oriRight > this.mListener.mScreenWidth) {
            oriRight = this.mListener.mScreenWidth;
            oriBottom2 = oriTop + ((int) (((oriRight - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriBottom2 > this.mListener.mFreeFormAccessibleArea.bottom) {
            oriBottom2 = this.mListener.mFreeFormAccessibleArea.bottom;
            oriRight = oriLeft + ((int) (((oriBottom2 - oriTop) * stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight > this.mListener.mFreeFormAccessibleArea.right && this.mListener.mDisplayRotation == 1) {
            oriRight = this.mListener.mFreeFormAccessibleArea.right;
            oriBottom2 = oriTop + ((int) (((oriRight - oriLeft) / stack.mWidthHeightScale) + 0.5f));
        }
        if (oriRight - oriLeft < stack.mMaxMinWidthSize[1] || oriRight - oriLeft > stack.mMaxMinWidthSize[0]) {
            if (oriRight - oriLeft < stack.mMaxMinWidthSize[1]) {
                if (!this.mInResizeMinSize) {
                    this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                    this.mInResizeMinSize = true;
                    this.mResizeMinX = x;
                }
                int adjustedWidth = stack.mMaxMinWidthSize[1] - ((int) afterFriction(Math.abs(x - this.mResizeMinX), stack.mMaxMinWidthSize[1]));
                oriRight = oriLeft + adjustedWidth;
                oriBottom2 = oriTop + ((int) ((adjustedWidth / stack.mWidthHeightScale) + 0.5f));
            } else if (oriRight - oriLeft > stack.mMaxMinWidthSize[0]) {
                if (!this.mInResizeMaxSize) {
                    this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                    this.mInResizeMaxSize = true;
                    this.mResizeMaxX = x;
                }
                int adjustedWidth2 = stack.mMaxMinWidthSize[0] + ((int) afterFriction(Math.abs(x - this.mResizeMaxX), stack.mMaxMinWidthSize[0]));
                oriRight = oriLeft + adjustedWidth2;
                oriBottom2 = oriTop + ((int) ((adjustedWidth2 / stack.mWidthHeightScale) + 0.5f));
            } else {
                this.mInResizeMinSize = false;
                this.mInResizeMaxSize = false;
            }
        }
        this.mResizeLastMoveLeft = oriLeft;
        this.mResizeLastMoveTop = oriTop;
        this.mResizeLastMoveRight = oriRight;
        this.mResizeLastMoveBottom = oriBottom2;
        Slog.i(TAG, "bottomRight");
        setLeashPositionAndScale(new Rect(oriLeft, oriTop, oriRight, oriBottom2), stack);
    }

    private void setLeashPositionAndScale(Rect currentPosition, MiuiFreeFormActivityStack stack) {
        float leashScale = currentPosition.width() / stack.mStackControlInfo.mScaleWindowWidth;
        Context context = this.mListener.mService.mContext;
        RectF possibleBounds = MiuiMultiWindowUtils.getPossibleBounds(context, this.mListener.mIsPortrait, stack.mIsLandcapeFreeform, stack.getStackPackageName());
        float resizeScale = currentPosition.width() / possibleBounds.width();
        this.mListener.mGestureAnimator.setPositionInTransaction(stack, currentPosition.left, currentPosition.top);
        this.mListener.mGestureAnimator.setMatrixInTransaction(stack, leashScale, 0.0f, 0.0f, leashScale);
        this.mListener.mGestureAnimator.applyTransaction();
        updateResizeParams(currentPosition.left, currentPosition.top, resizeScale, stack);
        stack.mStackControlInfo.mNowPosX = currentPosition.left;
        stack.mStackControlInfo.mNowPosY = currentPosition.top;
        stack.mStackControlInfo.mNowScale = leashScale;
        Slog.i(TAG, "setLeashPosition mResizeLeft:" + this.mResizeLeft + " ResizeTop: " + this.mResizeTop + " ResizeScale:" + this.mResizeScale + " currentScale:" + leashScale);
    }

    private void updateResizeParams(float left, float top, float scale, MiuiFreeFormActivityStack stack) {
        MiuiFreeFormActivityStack.StackControlInfo stackControlInfo = stack.mStackControlInfo;
        this.mResizeLeft = left;
        stackControlInfo.mNowPosX = left;
        MiuiFreeFormActivityStack.StackControlInfo stackControlInfo2 = stack.mStackControlInfo;
        this.mResizeTop = top;
        stackControlInfo2.mNowPosY = top;
        this.mResizeScale = scale;
    }

    public boolean notifyUpLocked(MotionEvent motionEvent, MiuiFreeFormActivityStack stack) {
        int i;
        if (stack == null || !stack.mStackBeenHandled) {
            return false;
        }
        float xVelocity = this.mVelocityMonitor.getVelocity(0);
        float yVelocity = this.mVelocityMonitor.getVelocity(1);
        stack.mStackControlInfo.mCurrentAction = 1;
        try {
            this.mCurrentX = motionEvent.getX();
            float y = motionEvent.getY();
            this.mCurrentY = y;
            int i2 = this.mCurrentTouchedMode;
            if (i2 == 0) {
                if (this.mShowedHotSpotView) {
                    try {
                        this.mListener.mGestureController.mWindowController.hideHotSpotView();
                    } catch (Exception e) {
                    }
                    this.mShowedHotSpotView = false;
                }
                int nX = (int) this.mCurrentX;
                int nY = (int) this.mCurrentY;
                this.mListener.mTaskPositioner.updateWindowUpBounds(nX, nY, this.mDownX, this.mDownY, xVelocity, yVelocity, this.mListener.mIsPortrait, stack.mTask.getBounds(), stack);
            } else if (i2 == 1) {
                float absSetY = Math.abs(y - this.mDownY);
                Slog.d(TAG, "notifyUpLocked absSetY:" + absSetY + " currentGesture:" + stack.mStackControlInfo.mCurrentGestureAction + " yVelocity= " + yVelocity);
                if (stack.mStackControlInfo.mCurrentGestureAction == 0) {
                    if (!this.mListener.mIsPortrait) {
                        if (absSetY >= LANDSCAPE_OPEN_OPERATION_THRESHOLD || yVelocity > 400.0f) {
                            this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME2, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                            if (this.mLandDropAnimation.get() == 2) {
                                this.mListener.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 3, stack);
                                this.mLandDropAnimation.set(-1);
                                resetBottomBarHotSpotState();
                                return true;
                            } else if (stack != null) {
                                this.mListener.mGestureController.mWindowController.setStartBounds(new Rect((int) stack.mStackControlInfo.mNowPosX, (int) stack.mStackControlInfo.mNowPosY, (int) (stack.mStackControlInfo.mNowPosX + (stack.mStackControlInfo.mScaleWindowWidth * stack.mStackControlInfo.mNowWidthScale)), (int) (stack.mStackControlInfo.mNowPosY + (stack.mStackControlInfo.mScaleWindowHeight * stack.mStackControlInfo.mNowHeightScale))));
                                this.mListener.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 3, stack);
                                this.mLandDropAnimation.set(-1);
                                resetBottomBarHotSpotState();
                                return true;
                            } else {
                                stack.mStackControlInfo.mCurrentAnimation = 1;
                            }
                        } else {
                            stack.mStackControlInfo.mCurrentAnimation = 2;
                        }
                    } else {
                        if (absSetY >= OPEN_OPERATION_THRESHOLD) {
                            if (stack.mIsLandcapeFreeform) {
                                this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME3, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                                if (this.mLandDropAnimation.get() == 2) {
                                    this.mListener.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 3, stack);
                                    this.mLandDropAnimation.set(-1);
                                    MiuiFreeFormWindowController.DropWindowType = 0;
                                    resetBottomBarHotSpotState();
                                    return true;
                                } else if (stack != null) {
                                    this.mListener.mGestureController.mWindowController.setStartBounds(new Rect((int) stack.mStackControlInfo.mNowPosX, (int) stack.mStackControlInfo.mNowPosY, (int) (stack.mStackControlInfo.mNowPosX + (stack.mStackControlInfo.mScaleWindowWidth * stack.mStackControlInfo.mNowWidthScale)), (int) (stack.mStackControlInfo.mNowPosY + (stack.mStackControlInfo.mScaleWindowHeight * stack.mStackControlInfo.mNowHeightScale))));
                                    this.mListener.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 3, stack);
                                    MiuiFreeFormWindowController.DropWindowType = 0;
                                    this.mLandDropAnimation.set(-1);
                                    resetBottomBarHotSpotState();
                                    return true;
                                }
                            } else if (this.mLandDropAnimation.get() == 2) {
                                this.mListener.mGestureController.mWindowController.startBorderAlphaHideAnimation();
                                this.mLandDropAnimation.set(-1);
                            }
                        }
                        if (absSetY > OPEN_OPERATION_THRESHOLD) {
                            this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME3, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                            stack.mStackControlInfo.mCurrentAnimation = 1;
                        } else {
                            stack.mStackControlInfo.mCurrentAnimation = 2;
                        }
                    }
                } else if (stack.mStackControlInfo.mCurrentGestureAction == 1) {
                    if (yVelocity <= CLOSE_OPERATION_THRESHOLD_VELOCITY_Y && absSetY > CLOSE_OPERATION_THRESHOLD_QUICK) {
                        this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME1, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                        stack.mStackControlInfo.mCurrentAnimation = 0;
                        this.mListener.hideInputMethodWindowIfNeeded();
                        this.mListener.mGestureDetector.hapticFeedback(268435461, false);
                    } else {
                        if (!MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext)) {
                            int isEnterHotArea = MiuiMultiWindowUtils.isEnterHotArea(this.mListener.mService.mContext, this.mCurrentX, this.mCurrentY, false, true);
                            this.mBottomBarHotSpotNum = isEnterHotArea;
                            if (isEnterHotArea != -1 && this.mListener.mGestureController.mMiuiFreeFormManagerService.getNotPinedMiniFreeFormActivityStack().isEmpty()) {
                                int i3 = this.mBottomBarHotSpotNum;
                                if (i3 == 1) {
                                    this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME4, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                                    stack.mStackControlInfo.mCurrentAnimation = 18;
                                    this.mListener.mSmallFreeFormWindowMotionHelper.startShowFreeFormWindow(stack);
                                } else if (i3 == 2) {
                                    this.mListener.mGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME4, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
                                    this.mListener.mSmallFreeFormWindowMotionHelper.startShowFreeFormWindow(stack);
                                    stack.mStackControlInfo.mCurrentAnimation = 19;
                                } else {
                                    stack.mStackControlInfo.mCurrentAnimation = 2;
                                }
                            }
                        }
                        stack.mStackControlInfo.mCurrentAnimation = 2;
                    }
                } else if (stack.mStackControlInfo.mCurrentGestureAction == -1) {
                    stack.mStackControlInfo.mCurrentAnimation = 2;
                }
                startGestureAnimation(stack.mStackControlInfo.mCurrentAnimation, stack);
                resetBottomBarHotSpotState();
            } else if (i2 == 2 || i2 == 3) {
                Slog.i(TAG, "Up mCurrentTouchedMode == TOUCH_RESIZE_LEFT || TOUCH_RESIZE_RIGHT ResizeLeft:" + this.mResizeLeft + " ResizeTop: " + this.mResizeTop + " ResizeScale:" + this.mResizeScale);
                int resizeBackMode = needResizeBack();
                if (resizeBackMode != 0 && resizeBackMode != 1) {
                    try {
                        float f = this.mResizeLeft;
                        Rect position = new Rect((int) f, (int) this.mResizeTop, (int) (f + stack.mTask.getBounds().width()), (int) (this.mResizeTop + stack.mTask.getBounds().height()));
                        finishAnimationControl(stack, position, this.mResizeScale, 2);
                        this.mListener.mGestureController.notifyFreeFormApplicationResizeEnd(SystemClock.uptimeMillis() - this.mResizeStartTime, stack);
                        Slog.i(TAG, "Up mCurrentTouchedMode == TOUCH_RESIZE_LEFT || TOUCH_RESIZE_RIGHT position:" + position);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                } else {
                    MiuiFreeFormActivityStack.StackControlInfo stackControlInfo = stack.mStackControlInfo;
                    if (this.mCurrentTouchedMode == 2) {
                        i = 10;
                    } else {
                        i = 11;
                    }
                    stackControlInfo.mCurrentAnimation = i;
                    this.mResizeBackMode = resizeBackMode;
                    startGestureAnimation(stack.mStackControlInfo.mCurrentAnimation, stack);
                }
            }
            this.mCurrentTouchedMode = -1;
            return true;
        } catch (Exception e3) {
            return false;
        }
    }

    private void resetBottomBarHotSpotState() {
        this.mListener.mGestureController.mWindowController.updateState(-1, MiuiFreeformBottomBarHotSpotView.BOTTOM_BAR_HOTSPOT_STATUS_HIDE, this.mCurrentX, this.mCurrentY);
        this.mEnteredTriggerBottomBarHotArea = false;
        this.mEnteredRemanderBottomBarHotSpotView = false;
        this.lastActivedBottomBarHotSpot = -1;
        this.mListener.mGestureController.mWindowController.hideBottomBarHotSpotView();
    }

    private int needResizeBack() {
        if (this.mInResizeMaxSize) {
            return 0;
        }
        if (this.mInResizeMinSize) {
            return 1;
        }
        return -1;
    }

    public void initGestureDetecteSpace(MiuiFreeFormActivityStack stack) {
        Rect freeFormVisualRect;
        int screenType = MiuiMultiWindowUtils.getScreenType(this.mListener.mService.mContext);
        Rect windowBounds = stack.mTask.getBounds();
        synchronized (this.mListener.mService.mGlobalLock) {
            WindowState imeWin = this.mListener.mService.mRoot.getCurrentInputMethodWindow();
            ActivityRecord topFullScreenActivity = stack.mTask.getTopFullscreenActivity();
            WindowState mainWindow = topFullScreenActivity != null ? topFullScreenActivity.findMainWindow() : null;
            if (imeWin != null && imeWin.isVisibleNow() && mainWindow != null) {
                freeFormVisualRect = MiuiMultiWindowUtils.getVisualBounds(mainWindow.getFrame(), stack.mFreeFormScale);
            } else {
                freeFormVisualRect = new Rect(windowBounds.left, windowBounds.top, windowBounds.left + ((int) stack.mStackControlInfo.mScaleWindowWidth), windowBounds.top + ((int) stack.mStackControlInfo.mScaleWindowHeight));
            }
        }
        this.mTopGestureDetectBounds.set(MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 0, freeFormVisualRect, this.mListener.mService.mContext));
        this.mBottomGestureDetectBounds.set(MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 1, freeFormVisualRect, this.mListener.mService.mContext));
        this.mLeftResizeGestureDetectBounds.set(MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 2, freeFormVisualRect, this.mListener.mService.mContext));
        this.mRightResizeGestureDetectBounds.set(MiuiMultiWindowUtils.getFreeFormHotSpots(screenType, 3, freeFormVisualRect, this.mListener.mService.mContext));
        Slog.w(TAG, "TopGestureDetectBounds:" + this.mTopGestureDetectBounds + " BottomGestureDetectBounds:" + this.mBottomGestureDetectBounds + " LeftResizeGestureDetectBounds:" + this.mLeftResizeGestureDetectBounds + " RightResizeGestureDetectBounds:" + this.mRightResizeGestureDetectBounds + " windowBounds:" + stack.mTask.getBounds());
    }

    private void dealWithWindowGestureAnimal(float x, float offsetY, int gestureAction, MiuiFreeFormActivityStack stack) {
        float per = (-offsetY) / (stack.mTask.getBounds().top + stack.mStackControlInfo.mScaleWindowHeight);
        if (per < 0.0f) {
            per = afterFrictionValue(per, 0.2f);
        }
        float[] scales = calculateWindowScale(offsetY, per, gestureAction, stack);
        float alpha = calculateWindowAlpha(gestureAction);
        float[] point = calculateWindowPosition(x, scales, stack.mStackControlInfo.mScaleWindowHeight, offsetY, per, gestureAction, stack);
        startAnimationSetParams(stack, scales, alpha, point);
    }

    private void startAnimationSetParams(MiuiFreeFormActivityStack stack, float[] scales, float alpha, float[] point) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock != null && animalLock.mCurrentAnimation != 20) {
            Slog.d(TAG, "startAnimationSetParams animalLock.mCurrentAnimation cancel: " + animalLock.mCurrentAnimation);
            animalLock.cancel();
        }
        if (animalLock == null || animalLock.mAlphaAnimation == null || animalLock.mScaleXAnimation == null || animalLock.mScaleYAnimation == null || animalLock.mTranslateYAnimation == null || animalLock.mTranslateXAnimation == null || animalLock.mCurrentAnimation != 20) {
            Slog.d(TAG, "bottom bar create Animation currentAlpha: " + stack.mStackControlInfo.mNowAlpha + " currentXScale: " + stack.mStackControlInfo.mNowWidthScale + " currentYScale: " + stack.mStackControlInfo.mNowHeightScale + " PosX: " + stack.mStackControlInfo.mNowPosX + " PosY: " + stack.mStackControlInfo.mNowPosY + " scales: " + scales + " alpha: " + alpha + " point: " + point);
            MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, alpha, 3947.8f, 0.99f, 0.0f, 1);
            MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, scales[0], 3947.8f, 0.99f, 0.0f, 5);
            MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, scales[1], 3947.8f, 0.99f, 0.0f, 6);
            MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, point[0], 3947.8f, 0.99f, 0.0f, 2);
            MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, point[1], 3947.8f, 0.99f, 0.0f, 3);
            alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
            scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
            scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
            tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
            tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
            animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
            this.mStackLocks.put(stack, animalLock);
            animalLock.mAlphaAnimation = alphaSpringAnimation;
            animalLock.mScaleXAnimation = scaleXSpringAnimation;
            animalLock.mScaleYAnimation = scaleYSpringAnimation;
            animalLock.mTranslateXAnimation = tXSpringAnimation;
            animalLock.mTranslateYAnimation = tYSpringAnimation;
        } else {
            Slog.d(TAG, "bottom bar Animation update scales: " + scales + " alpha: " + alpha + " point: " + point);
            animalLock.mAlphaAnimation.setStartValue(animalLock.mAlphaAnimation.getCurrentValue());
            animalLock.mAlphaAnimation.animateToFinalPosition(alpha);
            animalLock.mScaleXAnimation.setStartValue(animalLock.mScaleXAnimation.getCurrentValue());
            animalLock.mScaleXAnimation.animateToFinalPosition(scales[0]);
            animalLock.mScaleYAnimation.setStartValue(animalLock.mScaleYAnimation.getCurrentValue());
            animalLock.mScaleYAnimation.animateToFinalPosition(scales[1]);
            animalLock.mTranslateXAnimation.setStartValue(animalLock.mTranslateXAnimation.getCurrentValue());
            animalLock.mTranslateXAnimation.animateToFinalPosition(point[0]);
            animalLock.mTranslateYAnimation.setStartValue(animalLock.mTranslateYAnimation.getCurrentValue());
            animalLock.mTranslateYAnimation.animateToFinalPosition(point[1]);
        }
        animalLock.start(20);
    }

    public void turnFreeFormToSmallWindow(MiuiFreeFormActivityStack stack) {
        if (this.mShowedHotSpotView) {
            try {
                this.mListener.mGestureController.mWindowController.hideHotSpotView();
            } catch (Exception e) {
                Slog.d(TAG, "turnFreeFormToSmallWindow() hideHotSpotView fail", e);
            }
            this.mShowedHotSpotView = false;
        }
        startGestureAnimation(3, stack);
    }

    public void startFreeFormWindowAnimal(MiuiFreeFormActivityStack stack) {
        startGestureAnimation(4, stack);
    }

    public void startSmallWindowTranslateAnimal(int oldOrientation, int displayRotation, MiuiFreeFormActivityStack stack) {
        cancelAllSpringAnimal();
        Rect finalWindowBounds = new Rect();
        Rect oldSmallWindowBounds = stack.mStackControlInfo.mSmallWindowBounds;
        if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext)) {
            int statusBarHeight = MiuiMultiWindowUtils.getInsetValueFromServer(this.mListener.mGestureController.mDisplayContent.getDisplayUiContext(), WindowInsets.Type.statusBars());
            Rect availableAreaBefore = MiuiMultiWindowUtils.getFreeFormAccessibleAreaOnPad(this.mListener.mService.mContext, oldOrientation == 2, displayRotation, statusBarHeight);
            finalWindowBounds.set(oldSmallWindowBounds);
            MiuiMultiWindowUtils.adjustMiniFreeFormPosition(finalWindowBounds, availableAreaBefore, this.mListener.mFreeFormAccessibleArea);
        } else {
            finalWindowBounds.set(MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, 0.0f, 0.0f, MiuiMultiWindowUtils.mCurrentSmallWindowCorner, stack.mIsLandcapeFreeform));
        }
        if (finalWindowBounds.equals(oldSmallWindowBounds)) {
            return;
        }
        stack.mStackControlInfo.mSmallWindowBounds = new Rect(finalWindowBounds);
        stack.mStackControlInfo.mNowPosY = finalWindowBounds.top;
        stack.mStackControlInfo.mNowPosX = finalWindowBounds.left;
        this.mListener.mGestureAnimator.setPositionInTransaction(stack, finalWindowBounds.left, finalWindowBounds.top);
        this.mListener.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mSmallWindowTargetWScale, 0.0f, 0.0f, stack.mStackControlInfo.mSmallWindowTargetHScale);
        this.mListener.mGestureAnimator.applyTransaction();
        Slog.d(TAG, "startSmallWindowTranslateAnimal finalWindowBounds:" + finalWindowBounds + "mSmallWindowTargetWScale: " + stack.mStackControlInfo.mSmallWindowTargetWScale + " mSmallWindowTargetHScale: " + stack.mStackControlInfo.mSmallWindowTargetHScale);
        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
        synchronized (this.mListener.mService.mGlobalLock) {
            this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
        }
        inputTransaction.apply();
    }

    public void cancelAllSpringAnimal() {
        if (!this.mStackLocks.isEmpty()) {
            Set<Map.Entry<MiuiFreeFormActivityStack, MiuiFreeFormGestureAnimator.AnimalLock>> entrySet = this.mStackLocks.entrySet();
            for (Map.Entry<MiuiFreeFormActivityStack, MiuiFreeFormGestureAnimator.AnimalLock> winEntry : entrySet) {
                MiuiFreeFormGestureAnimator.AnimalLock animalLock = winEntry.getValue();
                animalLock.cancel();
            }
            this.mStackLocks.clear();
        }
    }

    public void startExitApplication(MiuiFreeFormActivityStack mStack) {
        synchronized (this.mListener.mService.mGlobalLock) {
            if (mStack != null) {
                if (mStack.mTask != null) {
                    ArraySet<Task> tasks = Sets.newArraySet(new Task[]{mStack.mTask});
                    this.mListener.mService.mTaskSnapshotController.snapshotTasks(tasks);
                }
            }
        }
        this.mListener.startExitApplication(mStack);
    }

    private void applyShowSmallWindowAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        synchronized (this.mListener.mService.mGlobalLock) {
            if (stack.getFreeFormConrolSurface() == null) {
                this.mListener.mGestureAnimator.createLeash(stack);
            }
        }
        float[] widthAndHeight = getSmallwindowWidthHeight(stack);
        stack.mStackControlInfo.mScaleWindowHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
        stack.mStackControlInfo.mScaleWindowWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
        stack.mStackControlInfo.mSmallWindowTargetWScale = widthAndHeight[0] / stack.mStackControlInfo.mScaleWindowWidth;
        stack.mStackControlInfo.mSmallWindowTargetHScale = widthAndHeight[1] / stack.mStackControlInfo.mScaleWindowHeight;
        float targetHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
        float targetWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
        if (!Float.isNaN(targetHeightScale) && !Float.isNaN(targetWidthScale)) {
            Rect targetCorner = (!stack.isLaunchedByCVW() || stack.mCVWControlInfo.miniFreeformBounds.isEmpty()) ? MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, this.mListener.mScreenWidth, this.mListener.mScreenHeight / 2, -1, stack.mIsLandcapeFreeform) : MiuiMultiWindowUtils.findCVWNearestCorner(this.mListener.mService.mContext, this.mListener.mScreenWidth, this.mListener.mScreenHeight / 2, stack.mIsLandcapeFreeform, stack.mCVWControlInfo.miniFreeformBounds);
            Slog.d(TAG, "PosX:" + stack.mStackControlInfo.mNowPosX + " PosY:" + stack.mStackControlInfo.mNowPosY + " targetCorner:" + targetCorner);
            MiuiFreeFormGestureAnimator.AnimalLock oldAnimalLock = this.mStackLocks.get(stack);
            if (oldAnimalLock != null && (oldAnimalLock.mCurrentAnimation == 20 || oldAnimalLock.mCurrentAnimation == 2)) {
                oldAnimalLock.mAlphaAnimation.setStartValue(oldAnimalLock.mAlphaAnimation.getCurrentValue());
                oldAnimalLock.mAlphaAnimation.animateToFinalPosition(1.0f);
                oldAnimalLock.mScaleXAnimation.setStartValue(oldAnimalLock.mScaleXAnimation.getCurrentValue());
                oldAnimalLock.mScaleXAnimation.animateToFinalPosition(targetWidthScale);
                oldAnimalLock.mScaleYAnimation.setStartValue(oldAnimalLock.mScaleYAnimation.getCurrentValue());
                oldAnimalLock.mScaleYAnimation.animateToFinalPosition(targetHeightScale);
                oldAnimalLock.mTranslateXAnimation.setStartValue(oldAnimalLock.mTranslateXAnimation.getCurrentValue());
                oldAnimalLock.mTranslateXAnimation.animateToFinalPosition(targetCorner.left);
                oldAnimalLock.mTranslateYAnimation.setStartValue(oldAnimalLock.mTranslateYAnimation.getCurrentValue());
                oldAnimalLock.mTranslateYAnimation.animateToFinalPosition(targetCorner.top);
                oldAnimalLock.mAlphaAnimation.getSpring().setStiffness(631.7f);
                oldAnimalLock.mAlphaAnimation.getSpring().setDampingRatio(0.7f);
                oldAnimalLock.mScaleXAnimation.getSpring().setStiffness(631.7f);
                oldAnimalLock.mScaleXAnimation.getSpring().setDampingRatio(0.7f);
                oldAnimalLock.mScaleYAnimation.getSpring().setStiffness(631.7f);
                oldAnimalLock.mScaleYAnimation.getSpring().setDampingRatio(0.7f);
                oldAnimalLock.mTranslateXAnimation.getSpring().setStiffness(631.7f);
                oldAnimalLock.mTranslateXAnimation.getSpring().setDampingRatio(0.7f);
                oldAnimalLock.mTranslateYAnimation.getSpring().setStiffness(631.7f);
                oldAnimalLock.mTranslateYAnimation.getSpring().setDampingRatio(0.7f);
                oldAnimalLock.start(animationType);
                return;
            }
            MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
            Rect targetCorner2 = targetCorner;
            MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, targetWidthScale, 631.7f, 0.7f, 0.0f, 5);
            MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, targetHeightScale, 631.7f, 0.7f, 0.0f, 6);
            MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mLastFreeFormWindowStartBounds.left, targetCorner2.left, 2000.0f, 0.99f, 0.0f, 2);
            MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mLastFreeFormWindowStartBounds.top, targetCorner2.top, 2000.0f, 0.99f, 0.0f, 3);
            scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
            scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
            tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
            tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
            this.mStackLocks.put(stack, animalLock);
            animalLock.mScaleXAnimation = scaleXSpringAnimation;
            animalLock.mScaleYAnimation = scaleYSpringAnimation;
            animalLock.mTranslateXAnimation = tXSpringAnimation;
            animalLock.mTranslateYAnimation = tYSpringAnimation;
            animalLock.start(animationType);
            return;
        }
        Slog.d(TAG, "applyShowSmallWindowAnimation invalid windowBounds " + stack.mTask.getBounds());
    }

    private void applyResetAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Slog.d(TAG, "applyResetAnimation stack: " + stack.getStackPackageName());
        MiuiFreeFormGestureAnimator.AnimalLock oldAnimalLock = this.mStackLocks.get(stack);
        if (oldAnimalLock != null && oldAnimalLock.mCurrentAnimation == 20) {
            oldAnimalLock.cancel();
            Slog.d(TAG, "applyResetAnimation cancel ANIMATION_BOTTOM_BAR_SET_PARAMS");
        }
        if (stack.mStackControlInfo.mCurrentGestureAction == 1) {
            float finalPosX = stack.mTask.getBounds().left;
            float startPosX = stack.mStackControlInfo.mNowPosX;
            float finalPosY = stack.mTask.getBounds().top;
            float startPosY = stack.mStackControlInfo.mNowPosY;
            MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 1.0f, 2000.0f, 0.99f, 0.0f, 1);
            MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, 1.0f, 2000.0f, 0.99f, 0.0f, 5);
            MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, 1.0f, 2000.0f, 0.99f, 0.0f, 6);
            MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosX, finalPosX, 2000.0f, 0.99f, 0.0f, 2);
            MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosY, finalPosY, 2000.0f, 0.99f, 0.0f, 3);
            alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
            scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
            scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
            tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
            tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
            MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
            this.mStackLocks.put(stack, animalLock);
            animalLock.mAlphaAnimation = alphaSpringAnimation;
            animalLock.mScaleXAnimation = scaleXSpringAnimation;
            animalLock.mScaleYAnimation = scaleYSpringAnimation;
            animalLock.mTranslateXAnimation = tXSpringAnimation;
            animalLock.mTranslateYAnimation = tYSpringAnimation;
            animalLock.start(2);
            stack.mStackControlInfo.mNowPosX = startPosX;
            stack.mStackControlInfo.mNowPosY = startPosY;
            return;
        }
        float finalPosX2 = stack.mTask.getBounds().left;
        float startPosX2 = stack.mStackControlInfo.mNowPosX;
        float finalPosY2 = stack.mTask.getBounds().top;
        float startPosY2 = stack.mStackControlInfo.mNowPosY;
        MiuiFreeFormSpringAnimation alphaSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 1.0f, 2000.0f, 0.99f, 0.0f, 1);
        MiuiFreeFormSpringAnimation scaleXSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, 1.0f, 2000.0f, 0.99f, 0.0f, 5);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, 1.0f, 2000.0f, 0.99f, 0.0f, 6);
        MiuiFreeFormSpringAnimation tXSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosX2, finalPosX2, 2000.0f, 0.99f, 0.0f, 2);
        MiuiFreeFormSpringAnimation tYSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosY2, finalPosY2, 2000.0f, 0.99f, 0.0f, 3);
        alphaSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
        scaleXSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        tXSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock2 = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock2);
        animalLock2.mAlphaAnimation = alphaSpringAnimation2;
        animalLock2.mScaleXAnimation = scaleXSpringAnimation2;
        animalLock2.mScaleYAnimation = scaleYSpringAnimation2;
        animalLock2.mTranslateXAnimation = tXSpringAnimation2;
        animalLock2.mTranslateYAnimation = tYSpringAnimation2;
        animalLock2.start(2);
        stack.mStackControlInfo.mNowPosX = startPosX2;
        stack.mStackControlInfo.mNowPosY = startPosY2;
    }

    private void applyResizeBackAnimation(MiuiFreeFormActivityStack stack, int resizeOrientation) {
        float f;
        float resizeScale;
        float finalPosY;
        float finalPosX;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        float startPosX = stack.mStackControlInfo.mNowPosX;
        float startPosY = stack.mStackControlInfo.mNowPosY;
        float startScale = stack.mStackControlInfo.mNowScale;
        if (this.mResizeBackMode == 0) {
            f = stack.mMaxMinWidthSize[0] / stack.mStackControlInfo.mScaleWindowWidth;
        } else {
            f = stack.mMaxMinWidthSize[1] / stack.mStackControlInfo.mScaleWindowWidth;
        }
        float finalScale = f;
        Context context = this.mListener.mService.mContext;
        RectF possibleBounds = MiuiMultiWindowUtils.getPossibleBounds(context, this.mListener.mIsPortrait, stack.mIsLandcapeFreeform, stack.getStackPackageName());
        float resizeScale2 = (stack.mStackControlInfo.mScaleWindowWidth * finalScale) / possibleBounds.width();
        Slog.d(TAG, "applyResizeBackAnimation() mResizeScale:" + this.mResizeScale);
        if (resizeOrientation == 10) {
            resizeScale = resizeScale2;
            float finalPosX2 = ((stack.mStackControlInfo.mScaleWindowWidth * startScale) + startPosX) - (stack.mStackControlInfo.mScaleWindowWidth * finalScale);
            Slog.i(TAG, "resizeOrientation == ANIMATION_RESIZE_BACK_LEFT_BOTTOM startScale:" + startScale + "finalScale:" + finalScale + "startPosX" + startPosX + "finalPosX:" + finalPosX2 + "startPosY" + startPosY + "mResizeScale:" + this.mResizeScale + "stack: " + stack);
            finalPosX = finalPosX2;
            finalPosY = startPosY;
        } else {
            resizeScale = resizeScale2;
            if (resizeOrientation == 11) {
                Slog.i(TAG, "resizeOrientation == ANIMATION_RESIZE_BACK_RIGHT_BOTTOM startScale:" + startScale + "finalScale:" + finalScale + "startPosX" + startPosX + "finalPosX:" + startPosX + "startPosY" + startPosY + "mResizeScale:" + this.mResizeScale + "stack: " + stack);
                finalPosX = startPosX;
                finalPosY = startPosY;
            } else {
                finalPosX = 0.0f;
                finalPosY = 0.0f;
            }
        }
        updateResizeParams(finalPosX, finalPosY, resizeScale, stack);
        MiuiFreeFormSpringAnimation scaleSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startScale, finalScale, 2000.0f, 0.99f, 0.0f, 4);
        scaleSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_END_LISTENER));
        this.mStackLocks.put(stack, animalLock);
        animalLock.mScaleAnimation = scaleSpringAnimation;
        animalLock.start(resizeOrientation);
    }

    private void applyBottomBarFreefromToSmallFreeformAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        float finalPosX;
        float finalPosY;
        Rect corner;
        Rect corner2;
        Rect corner3;
        Slog.d(TAG, "applyBottomBarFreefromToSmallFreeformAnimation stack: " + stack);
        MiuiFreeFormGestureAnimator.AnimalLock oldAnimalLock = this.mStackLocks.get(stack);
        if (oldAnimalLock != null && oldAnimalLock.mCurrentAnimation == 20) {
            oldAnimalLock.cancel();
            Slog.d(TAG, "applyBottomBarFreefromToSmallFreeformAnimation cancel ANIMATION_BOTTOM_BAR_SET_PARAMS");
        }
        float startPosX = stack.mStackControlInfo.mNowPosX;
        float startPosY = stack.mStackControlInfo.mNowPosY;
        float startXScale = stack.mStackControlInfo.mNowWidthScale;
        float startYScale = stack.mStackControlInfo.mNowHeightScale;
        Rect corner4 = new Rect();
        if (animationType == 18) {
            if (stack.isLaunchedByCVW() && !stack.mCVWControlInfo.miniFreeformBounds.isEmpty()) {
                corner3 = MiuiMultiWindowUtils.findCVWNearestCorner(this.mListener.mService.mContext, 0.0f, 0.0f, stack.mIsLandcapeFreeform, stack.mCVWControlInfo.miniFreeformBounds);
            } else {
                corner3 = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, 0.0f, 0.0f, 1, stack.mIsLandcapeFreeform);
            }
            finalPosX = corner3.left;
            finalPosY = corner3.top;
            corner = corner3;
        } else if (animationType != 19) {
            corner = corner4;
            finalPosX = 0.0f;
            finalPosY = 0.0f;
        } else {
            if (stack.isLaunchedByCVW() && !stack.mCVWControlInfo.miniFreeformBounds.isEmpty()) {
                corner2 = MiuiMultiWindowUtils.findCVWNearestCorner(this.mListener.mService.mContext, this.mListener.mScreenWidth, 0.0f, stack.mIsLandcapeFreeform, stack.mCVWControlInfo.miniFreeformBounds);
            } else {
                corner2 = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, 0.0f, 0.0f, 2, stack.mIsLandcapeFreeform);
            }
            finalPosX = corner2.left;
            finalPosY = corner2.top;
            corner = corner2;
        }
        float[] widthAndHeight = getSmallwindowWidthHeight(stack);
        float finalXScale = widthAndHeight[0] / stack.mStackControlInfo.mScaleWindowWidth;
        float finalYScale = widthAndHeight[1] / stack.mStackControlInfo.mScaleWindowHeight;
        Slog.i(TAG, "BottomBarFreefromToSmallFreeform mode: " + animationType + " startPosX: " + startPosX + " startPosY: " + startPosY + " startXScale: " + startXScale + " startYScale: " + startYScale + "finalPosX:" + finalPosX + "finalPosY" + finalPosY + "finalXScale:" + finalXScale + "finalYScale" + finalYScale + "stack: " + stack);
        Rect corner5 = corner;
        MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosX, finalPosX, 2000.0f, 0.99f, 0.0f, 2);
        MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosY, finalPosY, 2000.0f, 0.99f, 0.0f, 3);
        MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startXScale, finalXScale, 2000.0f, 0.99f, 0.0f, 5);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startYScale, finalYScale, 2000.0f, 0.99f, 0.0f, 6);
        tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock);
        animalLock.mTranslateXAnimation = tXSpringAnimation;
        animalLock.mTranslateYAnimation = tYSpringAnimation;
        animalLock.mScaleXAnimation = scaleXSpringAnimation;
        animalLock.mScaleYAnimation = scaleYSpringAnimation;
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                try {
                    animalLock.mStack.setStackFreeFormMode(1);
                    if (!corner5.isEmpty()) {
                        animalLock.mStack.mStackControlInfo.mSmallWindowBounds = new Rect(corner5);
                    }
                    SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
                    this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
                    inputTransaction.apply();
                    animalLock.start(animationType);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public MiuiFreeFormActivityStack findAnotherFreeFormStack(MiuiFreeFormActivityStack upperStack, Rect belowBounds) {
        List<MiuiFreeFormActivityStack> freeFormList = this.mListener.mGestureController.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
        if (freeFormList.size() > 2) {
            return null;
        }
        MiuiFreeFormActivityStack stackBelow = null;
        Iterator<MiuiFreeFormActivityStack> it = freeFormList.iterator();
        while (true) {
            if (it.hasNext()) {
                MiuiFreeFormActivityStack candidate = it.next();
                if (candidate != upperStack && candidate.mMiuiFreeFromWindowMode == 0 && !candidate.inPinMode()) {
                    stackBelow = candidate;
                    Slog.d(TAG, "stack below:" + stackBelow.toString());
                    break;
                }
            } else {
                break;
            }
        }
        if (stackBelow != null) {
            belowBounds.set(stackBelow.mTask.getBounds());
        }
        return stackBelow;
    }

    private void applyShowSmallToFreeFormWindowAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        float nowX;
        float nowY;
        float targetX;
        float targetY;
        float targetX2;
        float targetY2;
        Slog.d(TAG, "applyShowSmallToFreeFormWindowAnimation");
        float nowHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
        float nowWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
        float nowY2 = stack.mStackControlInfo.mSmallWindowBounds.top;
        float targetY3 = stack.mStackControlInfo.mLastFreeFormWindowStartBounds.top;
        stack.mStackControlInfo.mNowPosY = nowY2;
        float nowX2 = stack.mStackControlInfo.mSmallWindowBounds.left;
        float targetX3 = stack.mStackControlInfo.mLastFreeFormWindowStartBounds.left;
        stack.mStackControlInfo.mNowPosX = nowX2;
        if (!MiuiMultiWindowUtils.multiFreeFormSupported(this.mListener.mService.mContext)) {
            targetY2 = targetY3;
            targetX2 = targetX3;
            nowY = nowY2;
            nowX = nowX2;
        } else {
            Rect lowerStackBounds = new Rect();
            MiuiFreeFormActivityStack anotherStack = findAnotherFreeFormStack(stack, lowerStackBounds);
            if (anotherStack == null || anotherStack.mStackControlInfo.mCurrentAnimation == 19 || anotherStack.mStackControlInfo.mCurrentAnimation == 18 || anotherStack.mStackControlInfo.mCurrentAnimation == 12) {
                targetY2 = targetY3;
                targetX2 = targetX3;
                nowY = nowY2;
                nowX = nowX2;
            } else {
                Rect upperStackBounds = new Rect(stack.mStackControlInfo.mLastFreeFormWindowStartBounds);
                nowY = nowY2;
                Rect upperVisualBounds = new Rect(upperStackBounds.left, upperStackBounds.top, (int) (upperStackBounds.left + (upperStackBounds.width() * stack.mFreeFormScale) + 0.5f), (int) (upperStackBounds.top + (upperStackBounds.height() * stack.mFreeFormScale) + 0.5f));
                nowX = nowX2;
                Rect lowerVisualBounds = new Rect(lowerStackBounds.left, lowerStackBounds.top, (int) (lowerStackBounds.left + (anotherStack.mFreeFormScale * lowerStackBounds.width()) + 0.5f), (int) (lowerStackBounds.top + (anotherStack.mFreeFormScale * lowerStackBounds.height()) + 0.5f));
                MiuiMultiWindowUtils.avoidAsPossible(upperVisualBounds, lowerVisualBounds, MiuiMultiWindowUtils.getFreeFormAccessibleArea(this.mListener.mService.mContext));
                if (MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext)) {
                    anotherStack.avoidNewlyAddedStackIfNeeded(upperVisualBounds);
                }
                targetX = upperVisualBounds.left;
                targetY = upperVisualBounds.top;
                stack.mStackControlInfo.mTargetPosY = targetY;
                stack.mStackControlInfo.mTargetPosX = targetX;
                stack.mStackControlInfo.mLastFreeFormWindowStartBounds.offsetTo((int) targetX, (int) targetY);
                MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowWidthScale, 1.0f, 2000.0f, 0.99f, 0.0f, 5);
                MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowHeightScale, 1.0f, 2000.0f, 0.99f, 0.0f, 6);
                MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowX, targetX, 2000.0f, 0.99f, 0.0f, 2);
                MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowY, targetY, 2000.0f, 0.99f, 0.0f, 3);
                scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                this.mStackLocks.put(stack, animalLock);
                animalLock.mScaleXAnimation = scaleXSpringAnimation;
                animalLock.mScaleYAnimation = scaleYSpringAnimation;
                animalLock.mTranslateXAnimation = tXSpringAnimation;
                animalLock.mTranslateYAnimation = tYSpringAnimation;
                animalLock.start(5);
            }
        }
        targetY = targetY2;
        targetX = targetX2;
        stack.mStackControlInfo.mTargetPosY = targetY;
        stack.mStackControlInfo.mTargetPosX = targetX;
        stack.mStackControlInfo.mLastFreeFormWindowStartBounds.offsetTo((int) targetX, (int) targetY);
        MiuiFreeFormSpringAnimation scaleXSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowWidthScale, 1.0f, 2000.0f, 0.99f, 0.0f, 5);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowHeightScale, 1.0f, 2000.0f, 0.99f, 0.0f, 6);
        MiuiFreeFormSpringAnimation tXSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowX, targetX, 2000.0f, 0.99f, 0.0f, 2);
        MiuiFreeFormSpringAnimation tYSpringAnimation2 = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowY, targetY, 2000.0f, 0.99f, 0.0f, 3);
        scaleXSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        tXSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation2.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock2 = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock2);
        animalLock2.mScaleXAnimation = scaleXSpringAnimation2;
        animalLock2.mScaleYAnimation = scaleYSpringAnimation2;
        animalLock2.mTranslateXAnimation = tXSpringAnimation2;
        animalLock2.mTranslateYAnimation = tYSpringAnimation2;
        animalLock2.start(5);
    }

    private void applyLaunchSmallFreeFormWindow(final MiuiFreeFormActivityStack stack, int animationType) {
        this.mListener.mSmallFreeFormWindowMotionHelper.startShowFreeFormWindow(stack);
        Slog.d(TAG, "applyLaunchSmallFreeFormWindow");
        Rect corner = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, 0.0f, 0.0f, stack.mCornerPosition, stack.mIsLandcapeFreeform);
        float startScaleX = (this.mOriginalBounds.width() * 1.0f) / this.mListener.mScreenWidth;
        float startScaleY = (this.mOriginalBounds.height() * 1.0f) / this.mListener.mScreenHeight;
        float startLeft = this.mOriginalBounds.left + (this.mOriginalBounds.width() / 2);
        float startTop = this.mOriginalBounds.top + (this.mOriginalBounds.height() / 2);
        float endLeft = corner.left + (corner.width() / 2);
        float endTop = corner.top + (corner.height() / 2);
        float[] widthAndHeight = getSmallwindowWidthHeight(stack);
        float endScaleX = widthAndHeight[0] / this.mListener.mScreenWidth;
        float endScaleY = widthAndHeight[1] / this.mListener.mScreenHeight;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startScaleX, endScaleX, 438.6491f, 0.95f, 0.0f, 5, true);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startScaleY, endScaleY, 438.6491f, 0.95f, 0.0f, 6, true);
        MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startLeft, endLeft, 438.6491f, 0.9f, 0.0f, 2, true);
        MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startTop, endTop, 246.7401f, 0.9f, 0.0f, 3, true);
        scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        this.mStackLocks.put(stack, animalLock);
        animalLock.mScaleXAnimation = scaleXSpringAnimation;
        animalLock.mScaleYAnimation = scaleYSpringAnimation;
        animalLock.mTranslateXAnimation = tXSpringAnimation;
        animalLock.mTranslateYAnimation = tYSpringAnimation;
        animalLock.start(animationType, this.mListener.mGestureAnimator);
        this.mAlreadySetXScale = false;
        this.mAlreadySetYScale = false;
        new Thread(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper.1
            @Override // java.lang.Runnable
            public void run() {
                long timeoutAtTimeMs = System.currentTimeMillis() + 100;
                synchronized (MiuiFreeFormWindowMotionHelper.mLock) {
                    while (true) {
                        try {
                            if (MiuiFreeFormWindowMotionHelper.this.mAlreadySetXScale && MiuiFreeFormWindowMotionHelper.this.mAlreadySetYScale) {
                                break;
                            }
                            long waitMillis = timeoutAtTimeMs - System.currentTimeMillis();
                            if (waitMillis <= 0) {
                                break;
                            }
                            MiuiFreeFormWindowMotionHelper.mLock.wait(waitMillis);
                        } catch (InterruptedException e) {
                            MiuiFreeFormWindowMotionHelper.this.hideScreenSurface(stack);
                            Thread.currentThread().interrupt();
                        }
                    }
                    MiuiFreeFormWindowMotionHelper.this.hideScreenSurface(stack);
                }
            }
        }).start();
    }

    private void applyShowFreeFormWindowAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Slog.d(TAG, "applyShowFreeFormWindowAnimation");
        float nowHeightScale = stack.mStackControlInfo.mTargetHeightScale;
        float targetHeightScale = stack.mStackControlInfo.mNowHeightScale;
        stack.mStackControlInfo.mNowHeightScale = nowHeightScale;
        stack.mStackControlInfo.mTargetHeightScale = targetHeightScale;
        float nowWidthScale = stack.mStackControlInfo.mTargetWidthScale;
        float targetWidthScale = stack.mStackControlInfo.mNowWidthScale;
        stack.mStackControlInfo.mNowWidthScale = nowWidthScale;
        stack.mStackControlInfo.mTargetWidthScale = targetWidthScale;
        float nowY = stack.mStackControlInfo.mTargetPosY;
        float tempNowPosY = stack.mStackControlInfo.mNowPosY;
        stack.mStackControlInfo.mTargetPosY = tempNowPosY;
        stack.mStackControlInfo.mNowPosY = nowY;
        float nowX = stack.mStackControlInfo.mTargetPosX;
        float tempNowPosX = stack.mStackControlInfo.mNowPosX;
        stack.mStackControlInfo.mTargetPosX = tempNowPosX;
        stack.mStackControlInfo.mNowPosX = nowX;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowWidthScale, targetWidthScale, 2000.0f, 0.99f, 0.0f, 5);
            MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowHeightScale, targetHeightScale, 2000.0f, 0.99f, 0.0f, 6);
            MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowX, tempNowPosX, 2000.0f, 0.99f, 0.0f, 2);
            MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowY, tempNowPosY, 2000.0f, 0.99f, 0.0f, 3);
            MiuiFreeFormGestureAnimator.AnimalLock animalLock2 = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
            scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
            scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
            tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
            tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
            this.mStackLocks.put(stack, animalLock2);
            animalLock2.mScaleXAnimation = scaleXSpringAnimation;
            animalLock2.mScaleYAnimation = scaleYSpringAnimation;
            animalLock2.mTranslateXAnimation = tXSpringAnimation;
            animalLock2.mTranslateYAnimation = tYSpringAnimation;
            animalLock2.start(4);
            return;
        }
        animalLock.mTranslateXAnimation.getSpring().setStiffness(2000.0f);
        animalLock.mTranslateXAnimation.getSpring().setDampingRatio(0.99f);
        animalLock.mTranslateXAnimation.setStartValue(nowX);
        animalLock.mTranslateXAnimation.animateToFinalPosition(tempNowPosX);
        animalLock.mTranslateYAnimation.getSpring().setStiffness(2000.0f);
        animalLock.mTranslateYAnimation.getSpring().setDampingRatio(0.99f);
        animalLock.mTranslateYAnimation.setStartValue(nowY);
        animalLock.mTranslateYAnimation.animateToFinalPosition(tempNowPosY);
        animalLock.mScaleXAnimation.getSpring().setStiffness(2000.0f);
        animalLock.mScaleXAnimation.getSpring().setDampingRatio(0.99f);
        animalLock.mScaleXAnimation.setStartValue(nowWidthScale);
        animalLock.mScaleXAnimation.animateToFinalPosition(targetWidthScale);
        animalLock.mScaleYAnimation.getSpring().setStiffness(2000.0f);
        animalLock.mScaleYAnimation.getSpring().setDampingRatio(0.99f);
        animalLock.mScaleYAnimation.setStartValue(nowHeightScale);
        animalLock.mScaleYAnimation.animateToFinalPosition(targetHeightScale);
        animalLock.start(4);
    }

    private void applyCloseAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Slog.d(TAG, "applyCloseAnimation");
        synchronized (this.mListener.mService.mGlobalLock) {
            if (stack.getFreeFormConrolSurface() == null) {
                this.mListener.mGestureAnimator.createLeash(stack);
            }
        }
        MiuiFreeFormGestureAnimator.AnimalLock oldAnimalLock = this.mStackLocks.get(stack);
        if (oldAnimalLock != null && oldAnimalLock.mCurrentAnimation == 20) {
            Slog.d(TAG, "bottom bar applyCloseAnimation update");
            float finalPosX = (stack.mStackControlInfo.mNowPosX + (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) * stack.mStackControlInfo.mNowWidthScale)) - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) * 0.4f);
            float finalPosY = stack.mStackControlInfo.mNowPosY;
            oldAnimalLock.mAlphaAnimation.setStartValue(oldAnimalLock.mAlphaAnimation.getCurrentValue());
            oldAnimalLock.mAlphaAnimation.animateToFinalPosition(0.0f);
            oldAnimalLock.mScaleXAnimation.setStartValue(oldAnimalLock.mScaleXAnimation.getCurrentValue());
            oldAnimalLock.mScaleXAnimation.animateToFinalPosition(0.4f);
            oldAnimalLock.mScaleYAnimation.setStartValue(oldAnimalLock.mScaleYAnimation.getCurrentValue());
            oldAnimalLock.mScaleYAnimation.animateToFinalPosition(0.4f);
            oldAnimalLock.mTranslateXAnimation.setStartValue(oldAnimalLock.mTranslateXAnimation.getCurrentValue());
            oldAnimalLock.mTranslateXAnimation.animateToFinalPosition(finalPosX);
            oldAnimalLock.mTranslateYAnimation.setStartValue(oldAnimalLock.mTranslateYAnimation.getCurrentValue());
            oldAnimalLock.mTranslateYAnimation.animateToFinalPosition(finalPosY);
            oldAnimalLock.mAlphaAnimation.getSpring().setStiffness(322.2f);
            oldAnimalLock.mAlphaAnimation.getSpring().setDampingRatio(0.95f);
            oldAnimalLock.mScaleXAnimation.getSpring().setStiffness(322.2f);
            oldAnimalLock.mScaleXAnimation.getSpring().setDampingRatio(0.95f);
            oldAnimalLock.mScaleYAnimation.getSpring().setStiffness(322.2f);
            oldAnimalLock.mScaleYAnimation.getSpring().setDampingRatio(0.95f);
            oldAnimalLock.mTranslateXAnimation.getSpring().setStiffness(322.2f);
            oldAnimalLock.mTranslateXAnimation.getSpring().setDampingRatio(0.95f);
            oldAnimalLock.mTranslateYAnimation.getSpring().setStiffness(322.2f);
            oldAnimalLock.mTranslateYAnimation.getSpring().setDampingRatio(0.95f);
            oldAnimalLock.start(animationType);
            return;
        }
        stack.mStackControlInfo.mNowAlpha = 1.0f;
        stack.mStackControlInfo.mNowWidthScale = 1.0f;
        stack.mStackControlInfo.mNowHeightScale = 1.0f;
        stack.mStackControlInfo.mNowPosX = stack.mTask.getBounds().left;
        stack.mStackControlInfo.mNowPosY = stack.mTask.getBounds().top;
        float finalPosX2 = (stack.mStackControlInfo.mNowPosX + (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) * stack.mStackControlInfo.mNowWidthScale)) - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) * 0.4f);
        float finalPosY2 = stack.mStackControlInfo.mNowPosY;
        MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, 0.4f, 322.2f, 0.95f, 0.0f, 5);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, 0.4f, 322.2f, 0.95f, 0.0f, 6);
        MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 0.0f, 322.2f, 0.95f, 0.0f, 1);
        MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, finalPosX2, 322.2f, 0.95f, 0.0f, 2);
        MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, finalPosY2, 322.2f, 0.95f, 0.0f, 3);
        alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
        scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock);
        animalLock.mScaleXAnimation = scaleXSpringAnimation;
        animalLock.mScaleYAnimation = scaleYSpringAnimation;
        animalLock.mAlphaAnimation = alphaSpringAnimation;
        animalLock.mTranslateXAnimation = tXSpringAnimation;
        animalLock.mTranslateYAnimation = tYSpringAnimation;
        animalLock.start(animationType);
    }

    private void applyScaleHomeAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Slog.d(TAG, "applyScaleHomeAnimation");
        if (stack == null) {
            return;
        }
        synchronized (this.mListener.mService.mGlobalLock) {
            if (stack.getFreeFormConrolSurface() == null) {
                this.mListener.mGestureAnimator.createLeash(stack);
            }
        }
        MiuiFreeFormSpringAnimation scaleSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 0.9f, 1.0f, 438.6491f, 0.9f, 0.0f, 4);
        scaleSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock);
        animalLock.mScaleAnimation = scaleSpringAnimation;
        animalLock.start(animationType);
    }

    public ValueAnimator applyAlphaAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Slog.d(TAG, "applyAlphaAnimation animationType=" + animationType);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", 1.0f, 0.0f);
        ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(alpha);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(300L);
        animator.addUpdateListener(new GestureAnimatorUpdateListener(animator, stack, animationType));
        return animator;
    }

    public void startShadowAnimation(MiuiFreeFormActivityStack stack, boolean focuse, boolean freefromToSmall) {
        Slog.d(TAG, "startShadowAnimation focuse=" + focuse + " freefromToSmall=" + freefromToSmall);
        if (stack == null) {
            return;
        }
        if (freefromToSmall) {
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", 0.06f, 0.03f);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(alpha);
            this.mShadowAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setInterpolator(new LinearInterpolator());
            this.mShadowAnimator.setDuration(200L);
            ValueAnimator valueAnimator = this.mShadowAnimator;
            valueAnimator.addUpdateListener(new GestureAnimatorUpdateListener(valueAnimator, stack, 17));
            this.mShadowAnimator.start();
        } else if (focuse) {
            PropertyValuesHolder alpha2 = PropertyValuesHolder.ofFloat("ALPHA", 0.03f, 0.09f);
            ValueAnimator ofPropertyValuesHolder2 = ValueAnimator.ofPropertyValuesHolder(alpha2);
            this.mShadowAnimator = ofPropertyValuesHolder2;
            ofPropertyValuesHolder2.setInterpolator(new LinearInterpolator());
            this.mShadowAnimator.setDuration(200L);
            ValueAnimator valueAnimator2 = this.mShadowAnimator;
            valueAnimator2.addUpdateListener(new GestureAnimatorUpdateListener(valueAnimator2, stack, 16));
            this.mShadowAnimator.start();
        } else {
            PropertyValuesHolder alpha3 = PropertyValuesHolder.ofFloat("ALPHA", 0.09f, 0.03f);
            ValueAnimator ofPropertyValuesHolder3 = ValueAnimator.ofPropertyValuesHolder(alpha3);
            this.mShadowAnimator = ofPropertyValuesHolder3;
            ofPropertyValuesHolder3.setInterpolator(new LinearInterpolator());
            this.mShadowAnimator.setDuration(200L);
            ValueAnimator valueAnimator3 = this.mShadowAnimator;
            valueAnimator3.addUpdateListener(new GestureAnimatorUpdateListener(valueAnimator3, stack, 17));
            this.mShadowAnimator.start();
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:13:0x0096 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void applyOpenAnimation(java.util.Collection<android.animation.Animator> r30, int r31, com.android.server.wm.MiuiFreeFormActivityStack r32) {
        /*
            Method dump skipped, instructions count: 372
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.applyOpenAnimation(java.util.Collection, int, com.android.server.wm.MiuiFreeFormActivityStack):void");
    }

    /* JADX WARN: Incorrect condition in loop: B:16:0x0118 */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void applyDirectOpenAnimation(com.android.server.wm.MiuiFreeFormActivityStack r26, int r27) {
        /*
            Method dump skipped, instructions count: 518
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.applyDirectOpenAnimation(com.android.server.wm.MiuiFreeFormActivityStack, int):void");
    }

    public WindowState getTopWindow(int windowMode) {
        return getTopWindow(windowMode, true);
    }

    public WindowState getTopWindow(int windowMode, boolean shouldNotStopped) {
        WindowState window;
        try {
            Task rootTask = this.mListener.mGestureController.mDisplayContent.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(windowMode);
            ActivityRecord activityRecord = rootTask.getTopMostActivity();
            if (activityRecord != null && shouldNotStopped && !activityRecord.isState(ActivityRecord.State.STOPPED)) {
                synchronized (this.mListener.mService.mGlobalLock) {
                    window = activityRecord.findMainWindow(false);
                }
                return window;
            }
            return null;
        } catch (Exception e) {
            e.toString();
            return null;
        }
    }

    private void applyDims(float dimAmount, WindowState window) {
        if (window != null) {
            SurfaceControl.Transaction t = window.getPendingTransaction();
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "applyDims dimAmount=" + dimAmount + " window:" + window + " dimmerLayer:" + this.mDimmerLayer);
            }
            if (this.mDimmerLayer != null && window.getSurfaceControl() != null) {
                Rect dimBounds = window.getBounds();
                window.getPendingTransaction().setRelativeLayer(this.mDimmerLayer, window.getSurfaceControl(), 1);
                t.setPosition(this.mDimmerLayer, dimBounds.left, dimBounds.top);
                t.setWindowCrop(this.mDimmerLayer, dimBounds.width(), dimBounds.height());
                t.show(this.mDimmerLayer);
                t.setAlpha(this.mDimmerLayer, dimAmount);
                t.apply();
            }
        }
    }

    private void removeDimLayer() {
        SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mListener.mService.mTransactionFactory.get();
        if (this.mDimmerLayer != null) {
            Slog.d(TAG, "removeDimLayer dimmerLayer:" + this.mDimmerLayer);
            t.remove(this.mDimmerLayer);
            t.apply();
        }
    }

    public SurfaceControl makeDimLayer(WindowState window) {
        removeDimLayer();
        if (window == null || window.getTask() == null) {
            return null;
        }
        return window.getTask().makeChildSurface((WindowContainer) null).setParent(window.getTask().getSurfaceControl()).setColorLayer().setName("freeform Dim Layer for - " + window.getName()).build();
    }

    /* JADX WARN: Incorrect condition in loop: B:8:0x00bc */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void applySmallWindowOpenAnimation(com.android.server.wm.MiuiFreeFormActivityStack r26, int r27) {
        /*
            Method dump skipped, instructions count: 467
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.applySmallWindowOpenAnimation(com.android.server.wm.MiuiFreeFormActivityStack, int):void");
    }

    public void applyFlashBackLaunchFromBackGround(MiuiFreeFormActivityStack stack, Rect targetPosition) {
        if (stack == null) {
            return;
        }
        MiuiFreeFormSpringAnimation freeformTYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, targetPosition.top - 1000, targetPosition.top, 322.27f, 0.9f, 0.0f, 3);
        MiuiFreeFormSpringAnimation freeformTXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, targetPosition.left, targetPosition.left, 1.0f, 1.0f, 0.0f, 2);
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        this.mStackLocks.put(stack, animalLock);
        animalLock.mTranslateYAnimation = freeformTYSpringAnimation;
        animalLock.mTranslateXAnimation = freeformTXSpringAnimation;
        animalLock.mTranslateYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        animalLock.mTranslateXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        animalLock.start(22);
    }

    /* loaded from: classes.dex */
    public class GestureAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private int mAnimationType;
        private ValueAnimator mAnimator;
        private MiuiFreeFormActivityStack mStack;

        GestureAnimatorUpdateListener(ValueAnimator animator, MiuiFreeFormActivityStack stack, int animationType) {
            MiuiFreeFormWindowMotionHelper.this = r1;
            this.mAnimator = animator;
            this.mStack = stack;
            this.mAnimationType = animationType;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            try {
                Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "onAnimationUpdate animation:" + animation + " animationType:" + this.mAnimationType);
                if (this.mStack != null && this.mAnimator != null) {
                    int i = this.mAnimationType;
                    if (i != 0) {
                        if (i == 1) {
                            long duration = animation.getDuration();
                            long currentPlayTime = animation.getCurrentPlayTime();
                            if (currentPlayTime == 0) {
                                return;
                            }
                            if (currentPlayTime != 0 && MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime == 0) {
                                MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime = currentPlayTime;
                            }
                            long currentPlayTime2 = currentPlayTime - MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime;
                            if (currentPlayTime2 > duration) {
                                currentPlayTime2 = duration;
                            }
                            MiuiFreeFormGestureAnimator.TmpValues tmp = new MiuiFreeFormGestureAnimator.TmpValues();
                            MiuiFreeFormWindowMotionHelper.this.mScaleAnimator.getTransformation(currentPlayTime2, tmp.transformation);
                            Matrix matrix = tmp.transformation.getMatrix();
                            matrix.getValues(tmp.floats);
                            Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "apply currentPlayTime:" + currentPlayTime2 + " Matrix:" + matrix + " x:" + tmp.floats[2] + " y:" + tmp.floats[5] + " scaleY:" + tmp.floats[4] + " scaleX:" + tmp.floats[0]);
                            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setMatrixInTransaction(this.mStack, matrix, tmp.floats);
                        } else if (i != 2) {
                            if (i == 13) {
                                synchronized (MiuiFreeFormWindowMotionHelper.this.mSurfaceControlLock) {
                                    if (MiuiFreeFormWindowMotionHelper.this.mSurfaceControl != null && MiuiFreeFormWindowMotionHelper.this.mSurfaceControl.isValid()) {
                                        float value = ((Float) animation.getAnimatedValue("ALPHA")).floatValue();
                                        MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setAlphaInTransaction(MiuiFreeFormWindowMotionHelper.this.mSurfaceControl, value);
                                    }
                                }
                            } else if (i != 3 && i != 4 && i != 5) {
                                if (i == 16) {
                                    ((Float) animation.getAnimatedValue("ALPHA")).floatValue();
                                    MiuiFreeFormGestureAnimator miuiFreeFormGestureAnimator = MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator;
                                    MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mStack;
                                    miuiFreeFormGestureAnimator.setPositionInTransaction(miuiFreeFormActivityStack, miuiFreeFormActivityStack.mStackControlInfo.mNowPosX, this.mStack.mStackControlInfo.mNowPosY);
                                } else if (i == 17) {
                                    ((Float) animation.getAnimatedValue("ALPHA")).floatValue();
                                    MiuiFreeFormGestureAnimator miuiFreeFormGestureAnimator2 = MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator;
                                    MiuiFreeFormActivityStack miuiFreeFormActivityStack2 = this.mStack;
                                    miuiFreeFormGestureAnimator2.setPositionInTransaction(miuiFreeFormActivityStack2, miuiFreeFormActivityStack2.mStackControlInfo.mNowPosX, this.mStack.mStackControlInfo.mNowPosY);
                                } else if (i == 6) {
                                    long duration2 = animation.getDuration();
                                    long currentPlayTime3 = animation.getCurrentPlayTime();
                                    if (currentPlayTime3 == 0) {
                                        return;
                                    }
                                    if (currentPlayTime3 != 0 && MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime == 0) {
                                        MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime = currentPlayTime3;
                                    }
                                    long currentPlayTime4 = currentPlayTime3 - MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime;
                                    if (currentPlayTime4 > duration2) {
                                        currentPlayTime4 = duration2;
                                    }
                                    MiuiFreeFormGestureAnimator.TmpValues tmp2 = new MiuiFreeFormGestureAnimator.TmpValues();
                                    MiuiFreeFormWindowMotionHelper.this.mScaleAnimator.getTransformation(currentPlayTime4, tmp2.transformation);
                                    Matrix matrix2 = tmp2.transformation.getMatrix();
                                    matrix2.getValues(tmp2.floats);
                                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "apply currentPlayTime:" + currentPlayTime4 + " Matrix:" + matrix2 + " x:" + tmp2.floats[2] + " y:" + tmp2.floats[5] + " scaleY:" + tmp2.floats[4] + " scaleX:" + tmp2.floats[0]);
                                    MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setMatrixInTransaction(this.mStack, matrix2, tmp2.floats);
                                }
                            }
                        }
                    }
                    MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.applyTransaction();
                }
            } catch (Exception e) {
                Slog.w(MiuiFreeFormWindowMotionHelper.TAG, "onAnimationUpdate exception", e);
            }
        }
    }

    public void startGestureAnimation(int gestureAnimation, final MiuiFreeFormActivityStack stack) {
        Collection<Animator> animatorItems = new HashSet<>();
        stack.mStackControlInfo.mCurrentAnimation = gestureAnimation;
        Slog.w(TAG, "startGestureAnimation mCurrentAnimation:" + gestureAnimation);
        if (gestureAnimation == 1) {
            if (this.mListener.mIsPortrait) {
                this.mListener.hideInputMethodWindowIfNeeded();
                this.mListener.mGestureController.mMiuiFreeFormShadowHelper.resetShadowSettings(stack, true);
                applyOpenAnimation(animatorItems, gestureAnimation, stack);
            } else {
                stack.mStackControlInfo.mCurrentAnimation = -1;
            }
        } else if (gestureAnimation == 15) {
            this.mListener.hideInputMethodWindowIfNeeded();
            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.resetShadowSettings(stack, true);
            stack.isChangingFromFreeformToFullscreen = true;
            applyDirectOpenAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 6) {
            this.mListener.hideInputMethodWindowIfNeeded();
            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.resetShadowSettings(stack, true);
            applySmallWindowOpenAnimation(stack, gestureAnimation);
        }
        ValueAnimator a = null;
        if (gestureAnimation == 0) {
            applyCloseAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 2) {
            applyResetAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 3) {
            applyShowSmallWindowAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 4) {
            applyShowFreeFormWindowAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 5) {
            applyShowSmallToFreeFormWindowAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 10 || gestureAnimation == 11) {
            applyResizeBackAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 12) {
            applyLaunchSmallFreeFormWindow(stack, gestureAnimation);
        } else if (gestureAnimation == 13) {
            a = applyAlphaAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 14) {
            applyScaleHomeAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 18 || gestureAnimation == 19) {
            applyBottomBarFreefromToSmallFreeformAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 25) {
            applyFreeformPinAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 30) {
            applySmallFreeformPinAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 28) {
            applyFreeformUnPinAnimation(stack, gestureAnimation);
        } else if (gestureAnimation == 29) {
            applySmallFreeformUnPinAnimation(stack, gestureAnimation);
        }
        if (a != null) {
            animatorItems.add(a);
        }
        if (animatorItems.size() > 0) {
            AnimatorSet animatorSet = new AnimatorSet();
            this.mGestureAnimatorSet = animatorSet;
            animatorSet.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper.2
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                    Slog.w(MiuiFreeFormWindowMotionHelper.TAG, "start animation");
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    try {
                        if (stack.mStackControlInfo.mCurrentAnimation != 0) {
                            if (stack.mStackControlInfo.mCurrentAnimation == 1) {
                                MiuiFreeFormWindowMotionHelper.this.mListener.startShowFullScreenWindow(3, stack);
                            } else if (stack.mStackControlInfo.mCurrentAnimation != 2 && stack.mStackControlInfo.mCurrentAnimation != 3 && stack.mStackControlInfo.mCurrentAnimation != 4 && stack.mStackControlInfo.mCurrentAnimation != 5 && stack.mStackControlInfo.mCurrentAnimation != 6 && stack.mStackControlInfo.mCurrentAnimation == 13) {
                                MiuiFreeFormWindowMotionHelper.this.hideScreenSurface(stack);
                            }
                        }
                        Slog.w(MiuiFreeFormWindowMotionHelper.TAG, "mCurrentAnimation" + stack.mStackControlInfo.mCurrentAnimation + "end");
                        stack.mStackControlInfo.mCurrentAnimation = -1;
                    } catch (Exception e) {
                        Slog.w(MiuiFreeFormWindowMotionHelper.TAG, "Animation end exception", e);
                    }
                    MiuiFreeFormWindowMotionHelper.this.mAnimationStartTime = 0L;
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    Slog.w(MiuiFreeFormWindowMotionHelper.TAG, "cancel go back animation");
                }
            });
            this.mGestureAnimatorSet.playTogether(animatorItems);
            this.mGestureAnimatorSet.start();
        }
    }

    private void applyFreeformPinAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Throwable th;
        Slog.d(TAG, "applyFreeformPinAnimation");
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                if (stack.getFreeFormConrolSurface() == null) {
                    this.mListener.mGestureAnimator.createLeash(stack);
                }
            } catch (Throwable th2) {
                th = th2;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                throw th;
            }
        }
        ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
        if (activityRecord == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applyFreeformPinAnimation activityRecord == null");
        } else if (activityRecord.mFloatWindwoIconSurfaceControl == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applyFreeformPinAnimation activityRecord.mFloatWindwoIconSurfaceControl: " + activityRecord.mFloatWindwoIconSurfaceControl);
        } else {
            this.mListener.hideInputMethodWindowIfNeeded();
            this.mListener.reflectHandleSnapshotTaskByFreeform(stack.mTask);
            stack.mIsRunningPinAnim = true;
            stack.mStackControlInfo.mScaleWindowWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            stack.mStackControlInfo.mWindowBounds = new Rect(stack.mTask.getBounds());
            stack.mStackControlInfo.mNowAlpha = 1.0f;
            stack.mStackControlInfo.mNowPosX = stack.mTask.getBounds().left;
            stack.mStackControlInfo.mNowPosY = stack.mTask.getBounds().top;
            stack.mStackControlInfo.mNowWidthScale = 1.0f;
            stack.mStackControlInfo.mNowHeightScale = 1.0f;
            stack.mStackControlInfo.mNowClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            stack.mStackControlInfo.mNowClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
            stack.mStackControlInfo.mNowRoundCorner = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mListener.mService.mContext);
            stack.mStackControlInfo.mNowShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
            float finalClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / 0.7f;
            float finalClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / 0.7f;
            float finalRoundCornor = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / 0.7f;
            float finalPosX = stack.mPinFloatingWindowPos.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * 0.7f) / 2.0f);
            float finalPosY = stack.mPinFloatingWindowPos.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * 0.7f) / 2.0f);
            stack.mWindowRoundCorner = stack.mStackControlInfo.mNowRoundCorner;
            stack.mWindowScaleX = 1.0f;
            stack.mWindowScaleY = 1.0f;
            stack.setInPinMode(true);
            drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, 0.7f, 0.7f);
            synchronized (this.mListener.mService.mGlobalLock) {
                try {
                    WindowState mainWindow = activityRecord.findMainWindow();
                    if (mainWindow != null) {
                        try {
                            this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 1.0f);
                        } catch (Throwable th4) {
                            th = th4;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipHeight / 2.0f) + 0.5f)));
                    this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, stack.mStackControlInfo.mNowRoundCorner);
                    this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR, 0.0f, 0.0f, 0.0f, 1);
                    synchronized (this.mListener.mService.mGlobalLock) {
                        try {
                            this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0.0f);
                            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, 0.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.applyTransaction();
                    MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 0.0f, 322.27f, 0.9f, 0.0f, 1, true).setMaxValue(1.0f).setMinValue(0.0f);
                    MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, finalPosX, 109.7f, 0.78f, 0.0f, 2, true);
                    MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, finalPosY, 109.7f, 0.78f, 0.0f, 3, true);
                    MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, 0.7f, 322.27f, 0.9f, 0.0f, 5, true);
                    MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, 0.7f, 322.27f, 0.9f, 0.0f, 6, true);
                    MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipWidth, finalClipWidth, 322.27f, 0.9f, 0.0f, 8, true).setMaxValue(stack.mStackControlInfo.mNowClipWidth);
                    MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipHeight, finalClipHeight, 322.27f, 0.9f, 0.0f, 9, true).setMaxValue(stack.mStackControlInfo.mNowClipHeight);
                    MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowRoundCorner, finalRoundCornor, 322.27f, 0.9f, 0.0f, 7, true);
                    MiuiFreeFormSpringAnimation shadowRadiusSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowShadowAlpha, 0.0f, 322.27f, 0.9f, 0.0f, 10, true);
                    alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
                    tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                    tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                    scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                    scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                    clipWidthXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
                    clipHeightYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
                    roundCornerSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
                    shadowRadiusSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
                    MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                    this.mStackLocks.put(stack, animalLock);
                    animalLock.mScaleXAnimation = scaleXSpringAnimation;
                    animalLock.mScaleYAnimation = scaleYSpringAnimation;
                    animalLock.mAlphaAnimation = alphaSpringAnimation;
                    animalLock.mTranslateXAnimation = tXSpringAnimation;
                    animalLock.mTranslateYAnimation = tYSpringAnimation;
                    animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
                    animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
                    animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
                    animalLock.mShadowAlphaAnimation = shadowRadiusSpringAnimation;
                    animalLock.start(animationType, this.mListener.mGestureAnimator);
                } catch (Throwable th8) {
                    th = th8;
                }
            }
        }
    }

    private void applyFreeformUnPinAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        float nowGapY;
        float finalPosY;
        float finalClipHeight;
        float f;
        float finalPosX;
        float finalPosY2;
        float finalPosX2;
        Throwable th;
        float f2;
        float finalPosY3;
        float finalPosX3;
        float finalPosX4;
        float finalPosY4;
        Slog.d(TAG, "applyFreeformUnPinAnimation");
        if (stack.mIsRunningUnPinAnim) {
            Slog.d(TAG, "skip applyFreeformUnPinAnimation stack.mIsRunningUnPinAnim: true");
            return;
        }
        stack.mIsRunningUnPinAnim = true;
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                if (stack.getFreeFormConrolSurface() == null) {
                    this.mListener.mGestureAnimator.createLeash(stack);
                }
            } catch (Throwable th2) {
                th = th2;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                throw th;
            }
        }
        ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
        if (activityRecord == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applyFreeformUnPinAnimation activityRecord == null");
        } else if (activityRecord.mFloatWindwoIconSurfaceControl == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applyFreeformUnPinAnimation activityRecord.mFloatWindwoIconSurfaceControl: " + activityRecord.mFloatWindwoIconSurfaceControl);
        } else {
            synchronized (this.mListener.mService.mGlobalLock) {
                try {
                    this.mListener.mService.mAtmService.setFocusedTask(stack.mTask.mTaskId);
                    this.mListener.mService.updateFocusedWindowLocked(0, true);
                } catch (Throwable th4) {
                    th = th4;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th5) {
                            th = th5;
                        }
                    }
                    throw th;
                }
            }
            stack.mStackControlInfo.mNowAlpha = 0.0f;
            stack.mStackControlInfo.mNowWidthScale = 0.7f;
            stack.mStackControlInfo.mNowHeightScale = 0.7f;
            stack.mStackControlInfo.mNowPosX = stack.mPinFloatingWindowPos.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
            stack.mStackControlInfo.mNowPosY = stack.mPinFloatingWindowPos.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
            stack.mStackControlInfo.mNowClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / stack.mStackControlInfo.mNowWidthScale;
            stack.mStackControlInfo.mNowClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / stack.mStackControlInfo.mNowHeightScale;
            stack.mStackControlInfo.mNowRoundCorner = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / stack.mStackControlInfo.mNowWidthScale;
            stack.mStackControlInfo.mNowShadowAlpha = 0.0f;
            float finalRoundCornor = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mListener.mService.mContext);
            float finalClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            float finalClipHeight2 = stack.mTask.getBounds().height() * stack.mFreeFormScale;
            float finalShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
            float nowGapX = MiuiMultiWindowUtils.applyDip2Px(28.0f);
            if (this.mListener.mScreenWidth < this.mListener.mScreenHeight && (stack.mTask.getBounds().width() * stack.mFreeFormScale) + (nowGapX * 2.0f) > this.mListener.mScreenWidth) {
                nowGapX = (this.mListener.mScreenWidth - (stack.mTask.getBounds().width() * stack.mFreeFormScale)) / 2.0f;
            }
            float nowGapY2 = MiuiMultiWindowUtils.applyDip2Px(35.0f);
            if (this.mListener.mScreenWidth > this.mListener.mScreenHeight && (stack.mTask.getBounds().height() * stack.mFreeFormScale) + (nowGapY2 * 2.0f) > this.mListener.mScreenHeight) {
                nowGapY = (this.mListener.mScreenHeight - (stack.mTask.getBounds().height() * stack.mFreeFormScale)) / 2.0f;
            } else {
                nowGapY = nowGapY2;
            }
            if (stack.mIsEnterClick) {
                int displayCutoutHeight = MiuiFreeFormGestureDetector.getDisplayCutoutHeight(this.mListener.mGestureController.mDisplayContent.mDisplayFrames);
                if (stack.mPinFloatingWindowPos.left <= this.mListener.mScreenWidth / 2) {
                    if (this.mListener.mDisplayRotation == 1) {
                        nowGapX = Math.max(displayCutoutHeight, nowGapX);
                    }
                    finalPosX3 = Math.min(this.mListener.mScreenWidth - (stack.mTask.getBounds().width() * stack.mFreeFormScale), nowGapX);
                } else {
                    if (this.mListener.mDisplayRotation == 3) {
                        nowGapX = Math.max(displayCutoutHeight, nowGapX);
                    }
                    finalPosX3 = Math.max(0.0f, (this.mListener.mScreenWidth - nowGapX) - (stack.mTask.getBounds().width() * stack.mFreeFormScale));
                }
                float finalPosY5 = Math.max(nowGapY, Math.min(stack.mPinFloatingWindowPos.top, (this.mListener.mScreenHeight - nowGapY) - (stack.mTask.getBounds().height() * stack.mFreeFormScale)));
                if (!MiuiMultiWindowUtils.multiFreeFormSupported(this.mListener.mService.mContext)) {
                    finalPosY4 = finalPosY5;
                    finalPosX4 = finalPosX3;
                    finalClipHeight = finalClipHeight2;
                    finalPosY3 = finalClipWidth;
                } else {
                    Rect lowerStackBounds = new Rect();
                    MiuiFreeFormActivityStack anotherStack = findAnotherFreeFormStack(stack, lowerStackBounds);
                    if (anotherStack == null || anotherStack.mStackControlInfo.mCurrentAnimation == 19 || anotherStack.mStackControlInfo.mCurrentAnimation == 18 || anotherStack.mStackControlInfo.mCurrentAnimation == 12) {
                        finalPosY4 = finalPosY5;
                        finalPosX4 = finalPosX3;
                        finalClipHeight = finalClipHeight2;
                        finalPosY3 = finalClipWidth;
                    } else {
                        finalClipHeight = finalClipHeight2;
                        finalPosY3 = finalClipWidth;
                        Rect lowerVisualBounds = new Rect(lowerStackBounds.left, lowerStackBounds.top, (int) (lowerStackBounds.left + (anotherStack.mFreeFormScale * lowerStackBounds.width()) + 0.5f), (int) (lowerStackBounds.top + (anotherStack.mFreeFormScale * lowerStackBounds.height()) + 0.5f));
                        Rect upperStackBounds = new Rect(stack.mTask.getBounds());
                        upperStackBounds.offsetTo((int) (finalPosX3 + 0.5f), (int) (finalPosY5 + 0.5f));
                        Rect upperVisualBounds = new Rect(upperStackBounds.left, upperStackBounds.top, (int) (upperStackBounds.left + (upperStackBounds.width() * stack.mFreeFormScale) + 0.5f), (int) (upperStackBounds.top + (upperStackBounds.height() * stack.mFreeFormScale) + 0.5f));
                        int rotation = this.mListener.mGestureController.mDisplayContent.getRotation();
                        MiuiMultiWindowUtils.avoidAsPossible(upperVisualBounds, lowerVisualBounds, MiuiMultiWindowUtils.getFreeFormAccessibleArea(this.mListener.mService.mContext, rotation, MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController), MiuiFreeFormGestureDetector.getNavBarHeight(this.mListener.mGestureController.mInsetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(this.mListener.mGestureController.mDisplayContent.mDisplayFrames)));
                        finalPosX = upperVisualBounds.left;
                        finalPosY2 = upperVisualBounds.top;
                        f = 1.0f;
                    }
                }
                finalPosY2 = finalPosY4;
                finalPosX = finalPosX4;
                f = 1.0f;
            } else {
                finalClipHeight = finalClipHeight2;
                finalPosY3 = finalClipWidth;
                finalPosX = Math.max(nowGapX, Math.min(stack.mPinFloatingWindowPos.left, (this.mListener.mScreenWidth - nowGapX) - (stack.mTask.getBounds().width() * stack.mFreeFormScale)));
                finalPosY2 = Math.max(nowGapY, Math.min(stack.mPinFloatingWindowPos.top, (this.mListener.mScreenHeight - nowGapY) - (stack.mTask.getBounds().height() * stack.mFreeFormScale)));
                stack.mStackControlInfo.mNowAlpha = 0.0f;
                f = 1.0f;
                stack.mStackControlInfo.mNowWidthScale = 1.0f;
                stack.mStackControlInfo.mNowHeightScale = 1.0f;
                stack.mStackControlInfo.mNowPosX = stack.mPinFloatingWindowPos.left;
                stack.mStackControlInfo.mNowPosY = stack.mPinFloatingWindowPos.top;
                stack.mStackControlInfo.mNowClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
                stack.mStackControlInfo.mNowClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
                stack.mStackControlInfo.mNowRoundCorner = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mListener.mService.mContext);
            }
            if (MiuiMultiWindowUtils.isFullScreenGestureNav(this.mListener.mService.mContext)) {
                finalPosY = finalPosY2;
                finalPosX2 = finalPosX;
            } else {
                int navBarHeight = MiuiFreeFormGestureDetector.getNavBarHeight(this.mListener.mGestureController.mInsetsStateController);
                if (this.mListener.mIsPortrait) {
                    finalPosY = Math.min(finalPosY2, (this.mListener.mScreenHeight - navBarHeight) - (stack.mTask.getBounds().height() * stack.mFreeFormScale));
                    finalPosX2 = finalPosX;
                } else {
                    if (this.mListener.mDisplayRotation == 3 && finalPosX < navBarHeight) {
                        finalPosX = navBarHeight;
                    }
                    if (this.mListener.mDisplayRotation == 1 && (stack.mTask.getBounds().width() * stack.mFreeFormScale) + finalPosX > this.mListener.mScreenWidth - navBarHeight) {
                        finalPosY = finalPosY2;
                        finalPosX2 = (this.mListener.mScreenWidth - navBarHeight) - (stack.mTask.getBounds().width() * stack.mFreeFormScale);
                    } else {
                        finalPosY = finalPosY2;
                        finalPosX2 = finalPosX;
                    }
                }
            }
            drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, stack.mStackControlInfo.mNowWidthScale, stack.mStackControlInfo.mNowHeightScale);
            this.mListener.mGestureController.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(13, stack);
            this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
            this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipHeight / 2.0f) + 0.5f)));
            float f3 = f;
            this.mListener.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mNowWidthScale, 0.0f, 0.0f, stack.mStackControlInfo.mNowHeightScale);
            synchronized (this.mListener.mService.mGlobalLock) {
                try {
                    WindowState mainWindow = activityRecord.findMainWindow();
                    this.mListener.mGestureAnimator.setAlphaInTransaction(activityRecord.mFloatWindwoIconSurfaceControl, f3);
                    if (mainWindow != null) {
                        try {
                            this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 0.0f);
                            Slog.d(TAG, "applyFreeformUnPinAnimation setAlpha 0 mainWindow: " + mainWindow);
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, stack.mStackControlInfo.mNowRoundCorner);
                    float finalClipHeight3 = finalClipHeight;
                    float finalClipWidth2 = finalPosY;
                    this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                    synchronized (this.mListener.mService.mGlobalLock) {
                        try {
                            this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0.0f);
                            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, 0.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                        } finally {
                            th = th;
                            f2 = finalPosY;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th8) {
                                    th = th8;
                                }
                            }
                        }
                    }
                    this.mListener.mGestureAnimator.showStack(stack);
                    this.mListener.mGestureAnimator.applyTransaction();
                    MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 1.0f, 322.27f, 0.9f, 0.0f, 1, true).setMaxValue(f3).setMinValue(0.0f);
                    MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, finalPosX2, 322.27f, 0.9f, 0.0f, 2, true);
                    MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, finalPosY, 322.27f, 0.9f, 0.0f, 3, true);
                    MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, 1.0f, 322.27f, 0.9f, 0.0f, 5, true);
                    MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, 1.0f, 322.27f, 0.9f, 0.0f, 6, true);
                    MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipWidth, finalClipWidth2, 322.27f, 0.9f, 0.0f, 8, true).setMaxValue(finalClipWidth2);
                    MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipHeight, finalClipHeight3, 322.27f, 0.9f, 0.0f, 9, true).setMaxValue(finalClipHeight3);
                    MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowRoundCorner, finalRoundCornor, 322.27f, 0.9f, 0.0f, 7, true);
                    MiuiFreeFormSpringAnimation shadowRadiusSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowShadowAlpha, finalShadowAlpha, 322.27f, 0.9f, 0.0f, 10, true);
                    alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
                    tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                    tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                    scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                    scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                    clipWidthXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
                    clipHeightYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
                    roundCornerSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
                    shadowRadiusSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
                    MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                    this.mStackLocks.put(stack, animalLock);
                    animalLock.mAlphaAnimation = alphaSpringAnimation;
                    animalLock.mTranslateXAnimation = tXSpringAnimation;
                    animalLock.mTranslateYAnimation = tYSpringAnimation;
                    animalLock.mScaleXAnimation = scaleXSpringAnimation;
                    animalLock.mScaleYAnimation = scaleYSpringAnimation;
                    animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
                    animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
                    animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
                    animalLock.mShadowAlphaAnimation = shadowRadiusSpringAnimation;
                    animalLock.start(animationType, this.mListener.mGestureAnimator);
                } catch (Throwable th9) {
                    th = th9;
                }
            }
        }
    }

    private void applySmallFreeformPinAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        Throwable th;
        Slog.d(TAG, "applySmallFreeformPinAnimation");
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                if (stack.getFreeFormConrolSurface() == null) {
                    this.mListener.mGestureAnimator.createLeash(stack);
                }
            } catch (Throwable th2) {
                th = th2;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                throw th;
            }
        }
        ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
        if (activityRecord == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applySmallFreeformPinAnimation activityRecord == null");
        } else if (activityRecord.mFloatWindwoIconSurfaceControl == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applySmallFreeformPinAnimation activityRecord.mFloatWindwoIconSurfaceControl: " + activityRecord.mFloatWindwoIconSurfaceControl);
        } else {
            this.mListener.hideInputMethodWindowIfNeeded();
            this.mListener.reflectHandleSnapshotTaskByFreeform(stack.mTask);
            stack.mIsRunningPinAnim = true;
            stack.mStackControlInfo.mScaleWindowWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            stack.mStackControlInfo.mWindowBounds = new Rect(stack.mTask.getBounds());
            float maxScale = Math.max(stack.mStackControlInfo.mSmallWindowTargetHScale, stack.mStackControlInfo.mSmallWindowTargetWScale);
            float miniCornerRaduis = MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mListener.mService.mContext) / maxScale;
            stack.mStackControlInfo.mNowAlpha = 1.0f;
            stack.mStackControlInfo.mNowPosX = stack.mStackControlInfo.mSmallWindowBounds.left;
            stack.mStackControlInfo.mNowPosY = stack.mStackControlInfo.mSmallWindowBounds.top;
            stack.mStackControlInfo.mNowWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
            stack.mStackControlInfo.mNowHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
            stack.mStackControlInfo.mNowClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            stack.mStackControlInfo.mNowClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
            stack.mStackControlInfo.mNowRoundCorner = miniCornerRaduis;
            stack.mStackControlInfo.mNowShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
            float finalScaleX = stack.mStackControlInfo.mSmallWindowTargetWScale * 0.7f;
            float finalScaleY = stack.mStackControlInfo.mSmallWindowTargetHScale * 0.7f;
            float finalClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / finalScaleX;
            float finalClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / finalScaleY;
            float finalRoundCornor = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / finalScaleX;
            float finalPosX = stack.mPinFloatingWindowPos.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * finalScaleX) / 2.0f);
            float finalPosY = stack.mPinFloatingWindowPos.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * finalScaleY) / 2.0f);
            stack.mWindowRoundCorner = stack.mStackControlInfo.mNowRoundCorner;
            stack.mWindowScaleX = stack.mStackControlInfo.mSmallWindowTargetWScale;
            stack.mWindowScaleY = stack.mStackControlInfo.mSmallWindowTargetHScale;
            stack.setInPinMode(true);
            drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, finalScaleX, finalScaleY);
            WindowState mainWindow = activityRecord.findMainWindow();
            synchronized (this.mListener.mService.mGlobalLock) {
                if (mainWindow != null) {
                    try {
                        this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 1.0f);
                    } catch (Throwable th4) {
                        th = th4;
                        while (true) {
                            try {
                                break;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                        throw th;
                    }
                }
                try {
                    this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipHeight / 2.0f) + 0.5f)));
                    this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, stack.mStackControlInfo.mNowRoundCorner);
                    this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR, 0.0f, 0.0f, 0.0f, 1);
                    synchronized (this.mListener.mService.mGlobalLock) {
                        try {
                            this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0.0f);
                            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, 0.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.applyTransaction();
                    MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 0.0f, 322.27f, 0.9f, 0.0f, 1, true).setMaxValue(1.0f).setMinValue(0.0f);
                    MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, finalPosX, 109.7f, 0.78f, 0.0f, 2, true);
                    MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, finalPosY, 109.7f, 0.78f, 0.0f, 3, true);
                    MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, finalScaleX, 322.27f, 0.9f, 0.0f, 5, true);
                    MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, finalScaleY, 322.27f, 0.9f, 0.0f, 6, true);
                    MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipWidth, finalClipWidth, 322.27f, 0.9f, 0.0f, 8, true).setMaxValue(stack.mStackControlInfo.mNowClipWidth);
                    MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipHeight, finalClipHeight, 322.27f, 0.9f, 0.0f, 9, true).setMaxValue(stack.mStackControlInfo.mNowClipHeight);
                    MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowRoundCorner, finalRoundCornor, 322.27f, 0.9f, 0.0f, 7, true);
                    MiuiFreeFormSpringAnimation shadowRadiusSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowShadowAlpha, 0.0f, 322.27f, 0.9f, 0.0f, 10, true);
                    alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
                    tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                    tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                    scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                    scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                    clipWidthXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
                    clipHeightYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
                    roundCornerSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
                    shadowRadiusSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
                    MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                    this.mStackLocks.put(stack, animalLock);
                    animalLock.mScaleXAnimation = scaleXSpringAnimation;
                    animalLock.mScaleYAnimation = scaleYSpringAnimation;
                    animalLock.mAlphaAnimation = alphaSpringAnimation;
                    animalLock.mTranslateXAnimation = tXSpringAnimation;
                    animalLock.mTranslateYAnimation = tYSpringAnimation;
                    animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
                    animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
                    animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
                    animalLock.mShadowAlphaAnimation = shadowRadiusSpringAnimation;
                    animalLock.start(animationType, this.mListener.mGestureAnimator);
                } catch (Throwable th8) {
                    th = th8;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            }
        }
    }

    private void applySmallFreeformUnPinAnimation(MiuiFreeFormActivityStack stack, int animationType) {
        float finalPosY;
        float finalPosX;
        Throwable th;
        float finalPosX2;
        Slog.d(TAG, "applySmallFreeformUnPinAnimation");
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                if (stack.getFreeFormConrolSurface() == null) {
                    this.mListener.mGestureAnimator.createLeash(stack);
                }
            } catch (Throwable th2) {
                th = th2;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                }
                throw th;
            }
        }
        ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
        if (activityRecord == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applySmallFreeformUnPinAnimation activityRecord == null");
        } else if (activityRecord.mFloatWindwoIconSurfaceControl == null) {
            stack.mStackControlInfo.mCurrentAnimation = -1;
            Slog.d(TAG, "skip applySmallFreeformUnPinAnimation activityRecord.mFloatWindwoIconSurfaceControl: " + activityRecord.mFloatWindwoIconSurfaceControl);
        } else {
            stack.mIsRunningUnPinAnim = true;
            stack.mStackBeenHandled = false;
            float maxScale = Math.max(stack.mStackControlInfo.mSmallWindowTargetHScale, stack.mStackControlInfo.mSmallWindowTargetWScale);
            float miniCornerRaduis = MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mListener.mService.mContext) / maxScale;
            stack.mStackControlInfo.mNowShadowAlpha = 0.0f;
            if (stack.mIsEnterClick) {
                stack.mStackControlInfo.mNowPosX = stack.mPinFloatingWindowPos.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
                stack.mStackControlInfo.mNowPosY = stack.mPinFloatingWindowPos.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
                stack.mStackControlInfo.mNowWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale * 0.7f;
                stack.mStackControlInfo.mNowHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale * 0.7f;
                stack.mStackControlInfo.mNowClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / stack.mStackControlInfo.mNowWidthScale;
                stack.mStackControlInfo.mNowClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / stack.mStackControlInfo.mNowHeightScale;
                stack.mStackControlInfo.mNowRoundCorner = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / stack.mStackControlInfo.mNowWidthScale;
            } else {
                stack.mStackControlInfo.mNowAlpha = 0.0f;
                stack.mStackControlInfo.mNowPosX = stack.mPinFloatingWindowPos.left;
                stack.mStackControlInfo.mNowPosY = stack.mPinFloatingWindowPos.top;
                stack.mStackControlInfo.mNowWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
                stack.mStackControlInfo.mNowHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
                stack.mStackControlInfo.mNowClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
                stack.mStackControlInfo.mNowClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
                stack.mStackControlInfo.mNowRoundCorner = miniCornerRaduis;
            }
            Rect cornerPoint = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, stack.mPinFloatingWindowPos.centerX(), stack.mPinFloatingWindowPos.centerY(), -1, 0.0f, 0.0f, stack.mIsLandcapeFreeform);
            float finalPosX3 = cornerPoint.left;
            float finalPosY2 = cornerPoint.top;
            if (!MiuiMultiWindowUtils.isPadScreen(this.mListener.mService.mContext)) {
                finalPosX = finalPosX3;
                finalPosY = finalPosY2;
            } else if (stack.mIsEnterClick) {
                if (stack.mPinFloatingWindowPos.left > this.mListener.mScreenWidth / 2) {
                    finalPosX2 = (this.mListener.mScreenWidth - MiuiMultiWindowUtils.applyDip2Px(28.0f)) - stack.mStackControlInfo.mSmallWindowBounds.width();
                } else {
                    finalPosX2 = MiuiMultiWindowUtils.applyDip2Px(28.0f);
                }
                float finalPosY3 = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(stack.mPinFloatingWindowPos.centerY() - (stack.mStackControlInfo.mSmallWindowBounds.height() / 2), (this.mListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - stack.mStackControlInfo.mSmallWindowBounds.height()));
                finalPosX = finalPosX2;
                finalPosY = finalPosY3;
            } else {
                float finalPosX4 = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(stack.mPinFloatingWindowPos.left, (this.mListener.mScreenWidth - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - stack.mStackControlInfo.mSmallWindowBounds.width()));
                float finalPosY4 = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(stack.mPinFloatingWindowPos.top, (this.mListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - stack.mStackControlInfo.mSmallWindowBounds.height()));
                finalPosX = finalPosX4;
                finalPosY = finalPosY4;
            }
            float finalScaleX = stack.mStackControlInfo.mSmallWindowTargetWScale;
            float finalScaleY = stack.mStackControlInfo.mSmallWindowTargetHScale;
            float finalClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
            float finalClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
            float finalShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
            drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, stack.mStackControlInfo.mNowWidthScale, stack.mStackControlInfo.mNowHeightScale);
            this.mListener.mGestureController.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(13, stack);
            this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
            this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipHeight / 2.0f) + 0.5f)));
            this.mListener.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mNowWidthScale, 0.0f, 0.0f, stack.mStackControlInfo.mNowHeightScale);
            synchronized (this.mListener.mService.mGlobalLock) {
                try {
                    WindowState mainWindow = activityRecord.findMainWindow();
                    this.mListener.mGestureAnimator.setAlphaInTransaction(activityRecord.mFloatWindwoIconSurfaceControl, 1.0f);
                    if (mainWindow != null) {
                        try {
                            this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 0.0f);
                        } catch (Throwable th4) {
                            th = th4;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, stack.mStackControlInfo.mNowRoundCorner);
                    this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                    synchronized (this.mListener.mService.mGlobalLock) {
                        try {
                            this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0.0f);
                            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, 0.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, 0.0f, 0.0f, 0.0f, 1);
                        } catch (Throwable th6) {
                            th = th6;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th7) {
                                    th = th7;
                                }
                            }
                            throw th;
                        }
                    }
                    this.mListener.mGestureAnimator.showStack(stack);
                    this.mListener.mGestureAnimator.applyTransaction();
                    MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowAlpha, 1.0f, 322.27f, 0.9f, 0.0f, 1, true).setMaxValue(1.0f).setMinValue(0.0f);
                    MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosX, finalPosX, 109.7f, 0.78f, 0.0f, 2, true);
                    MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowPosY, finalPosY, 109.7f, 0.78f, 0.0f, 3, true);
                    MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, finalScaleX, 322.27f, 0.9f, 0.0f, 5, true);
                    MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, finalScaleY, 322.27f, 0.9f, 0.0f, 6, true);
                    MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipWidth, finalClipWidth, 322.27f, 0.9f, 0.0f, 8, true);
                    MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowClipHeight, finalClipHeight, 322.27f, 0.9f, 0.0f, 9, true);
                    MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowRoundCorner, miniCornerRaduis, 322.27f, 0.9f, 0.0f, 7, true);
                    MiuiFreeFormSpringAnimation shadowRadiusSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowShadowAlpha, finalShadowAlpha, 322.27f, 0.9f, 0.0f, 10, true);
                    alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
                    tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                    tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                    scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                    scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                    clipWidthXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
                    clipHeightYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
                    roundCornerSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
                    shadowRadiusSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
                    MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                    this.mStackLocks.put(stack, animalLock);
                    animalLock.mAlphaAnimation = alphaSpringAnimation;
                    animalLock.mTranslateXAnimation = tXSpringAnimation;
                    animalLock.mTranslateYAnimation = tYSpringAnimation;
                    animalLock.mScaleXAnimation = scaleXSpringAnimation;
                    animalLock.mScaleYAnimation = scaleYSpringAnimation;
                    animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
                    animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
                    animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
                    animalLock.mShadowAlphaAnimation = shadowRadiusSpringAnimation;
                    animalLock.start(animationType, this.mListener.mGestureAnimator);
                } catch (Throwable th8) {
                    th = th8;
                }
            }
        }
    }

    float afterFriction(float x, float range) {
        float per = Math.min(x / range, 1.0f);
        return ((((((per * 13.0f) * per) * per) / 75.0f) - (((per * 13.0f) * per) / 25.0f)) + ((13.0f * per) / 25.0f)) * range;
    }

    private float valFromPer(float per, float from, float to) {
        return ((to - from) * per) + from;
    }

    private float laterFriction(float per, float startFri, float distance) {
        if (per < startFri) {
            return per;
        }
        float offsetY = afterFrictionValue(per - startFri, distance) + startFri;
        return offsetY;
    }

    private float afterFrictionValue(float val, float range) {
        if (range <= 0.0f) {
            return 0.0f;
        }
        float t = val >= 0.0f ? 1.0f : -1.0f;
        float per = Math.max(Math.min(Math.abs(val) / range, 1.0f), 0.0f);
        return (((((per * per) * per) / 3.0f) - (per * per)) + per) * t * range;
    }

    private float cubicOut(float per) {
        return getEaseRatio(per, 1, 2);
    }

    private float getEaseRatio(float p, int type, int power) {
        float r;
        if (type == 1) {
            r = 1.0f - p;
        } else if (type != 2) {
            if (p < 0.5d) {
                r = p * 2.0f;
            } else {
                r = (1.0f - p) * 2.0f;
            }
        } else {
            r = p;
        }
        if (power == 1) {
            r *= r;
        } else if (power == 2) {
            r *= r * r;
        } else if (power == 3) {
            r *= r * r * r;
        } else if (power == 4) {
            r *= r * r * r * r;
        }
        if (type == 1) {
            return 1.0f - r;
        }
        if (type != 2) {
            if (p < 0.5d) {
                return r / 2.0f;
            }
            return 1.0f - (r / 2.0f);
        }
        return r;
    }

    private float[] calculateWindowScale(float offsetY, float per, int gestureAction, MiuiFreeFormActivityStack stack) {
        float[] scales = {1.0f, 1.0f};
        if (gestureAction == 0) {
            float range = (this.mListener.mScreenHeight - stack.mTask.getBounds().top) - stack.mStackControlInfo.mScaleWindowHeight;
            float afterFriction = (afterFriction(offsetY, range) / stack.mStackControlInfo.mScaleWindowHeight) + 1.0f;
            scales[1] = afterFriction;
            scales[0] = afterFriction;
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "GESTURE_ACTION_DOWN calculateWindowScale isPortrait" + this.mListener.mIsPortrait + "scales = :" + scales + " offsetY:" + offsetY + " maxScale:0.0 range:" + range);
            }
        } else if (gestureAction == 1) {
            float p = valFromPer(stack.mStackControlInfo.mScaleWindowWidth / this.mListener.mScreenWidth, 0.0f, 0.5f);
            scales[0] = 1.0f - laterFriction(per, p, 1.0f);
            scales[1] = scales[0];
            Slog.d(TAG, "GESTURE_ACTION_UP calculateWindowScale currentScale:" + scales);
        }
        return scales;
    }

    private float calculateWindowAlpha(int gestureAction) {
        return 1.0f;
    }

    private float[] calculateWindowPosition(float x, float[] scales, float windowHeight, float offsetY, float per, int gestureAction, MiuiFreeFormActivityStack stack) {
        float offsetY2 = offsetY;
        float[] point = {stack.mTask.getBounds().left, stack.mTask.getBounds().top};
        if (gestureAction == 0) {
            if (offsetY2 > 0.0f) {
                offsetY2 = afterFriction(offsetY2, stack.mTask.getBounds().height());
            }
            point[0] = x - (this.mProvitXOffset * scales[0]);
            point[1] = (stack.mTask.getBounds().top + windowHeight) - ((windowHeight - offsetY2) * scales[1]);
        } else if (gestureAction == 1) {
            point[0] = x - (this.mProvitXOffset * scales[0]);
            point[1] = ((stack.mTask.getBounds().top + windowHeight) * (1.0f - laterFriction(per, 0.4f, 6.0f))) - (scales[1] * windowHeight);
        }
        float effect = cubicOut(Math.min(1.0f, per));
        point[0] = Math.min(point[0], valFromPer(effect, this.mListener.mScreenWidth * 1.5f, (this.mListener.mScreenWidth - 20) - (stack.mStackControlInfo.mScaleWindowWidth * scales[0])));
        point[0] = Math.max(point[0], valFromPer(effect, (-this.mListener.mScreenWidth) * 0.5f, 20.0f));
        point[1] = Math.max(point[1], 20.0f);
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "calculateWindowAlpha currentTempY" + point[1] + " currentTempX:" + point[0]);
        }
        return point;
    }

    public void setAlpha(MiuiFreeFormActivityStack stack, float alpha) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setAlpha value:" + alpha + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null || animalLock.mCurrentAnimation == 6) {
            return;
        }
        if (animalLock.mCurrentAnimation == 20) {
            this.mListener.mGestureAnimator.setAlphaInTransaction(stack, alpha);
            stack.mStackControlInfo.mNowAlpha = alpha;
        } else if (animalLock.mCurrentAnimation == 25 || animalLock.mCurrentAnimation == 30 || animalLock.mCurrentAnimation == 26 || animalLock.mCurrentAnimation == 27 || animalLock.mCurrentAnimation == 28 || animalLock.mCurrentAnimation == 29) {
            ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
            synchronized (this.mListener.mService.mGlobalLock) {
                WindowState mainWindow = activityRecord.findMainWindow();
                if (activityRecord != null && mainWindow != null) {
                    if (animalLock.mCurrentAnimation == 28 && !stack.mIsEnterClick) {
                        this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, 1.0f);
                        Slog.d(TAG, "setAlpha mainWindow: " + mainWindow + "  alpha:1");
                    }
                    this.mListener.mGestureAnimator.setAlphaInTransaction(mainWindow.mSurfaceControl, alpha);
                    Slog.d(TAG, "setAlpha mainWindow: " + mainWindow + "  alpha:" + alpha);
                }
                if (activityRecord != null && activityRecord.mFloatWindwoIconSurfaceControl != null) {
                    this.mListener.mGestureAnimator.setAlphaInTransaction(activityRecord.mFloatWindwoIconSurfaceControl, 1.0f - alpha);
                }
            }
            stack.mStackControlInfo.mNowAlpha = alpha;
        } else {
            this.mListener.mGestureAnimator.setAlphaInTransaction(stack, alpha);
        }
        if (animalLock.mCurrentAnimation == 0 && alpha < 0.05f) {
            animalLock.cancel();
        }
    }

    public void setRoundCorner(MiuiFreeFormActivityStack stack, float roundCorner) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setRoundCorner value:" + roundCorner + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (animalLock.mCurrentAnimation == 25 || animalLock.mCurrentAnimation == 30 || animalLock.mCurrentAnimation == 26 || animalLock.mCurrentAnimation == 27 || animalLock.mCurrentAnimation == 28 || animalLock.mCurrentAnimation == 29) {
            this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, roundCorner);
            stack.mStackControlInfo.mNowRoundCorner = roundCorner;
        }
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setClipWidthX(MiuiFreeFormActivityStack stack, float value) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setClipWidthX value:" + value + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        stack.mStackControlInfo.mNowClipWidth = value;
        if (animalLock.mCurrentAnimation == 28 || animalLock.mCurrentAnimation == 29 || animalLock.mCurrentAnimation == 25 || animalLock.mCurrentAnimation == 30 || animalLock.mCurrentAnimation == 26 || animalLock.mCurrentAnimation == 27) {
            this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (value / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (value / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipHeight / 2.0f) + 0.5f)));
        }
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setClipHeightY(MiuiFreeFormActivityStack stack, float value) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setClipWidthY value:" + value + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        stack.mStackControlInfo.mNowClipHeight = value;
        if (animalLock.mCurrentAnimation == 28 || animalLock.mCurrentAnimation == 29 || animalLock.mCurrentAnimation == 25 || animalLock.mCurrentAnimation == 30 || animalLock.mCurrentAnimation == 26 || animalLock.mCurrentAnimation == 27) {
            this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (stack.mStackControlInfo.mNowClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (value / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (stack.mStackControlInfo.mNowClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (value / 2.0f) + 0.5f)));
        }
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setShadowAlpha(MiuiFreeFormActivityStack stack, float value) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setShadowAlpha value:" + value + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (animalLock.mCurrentAnimation == 25 || animalLock.mCurrentAnimation == 30 || animalLock.mCurrentAnimation == 26 || animalLock.mCurrentAnimation == 27 || animalLock.mCurrentAnimation == 28 || animalLock.mCurrentAnimation == 29) {
            float[] ambientColor = {0.0f, 0.0f, 0.0f, value};
            this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, ambientColor, 0.0f, 0.0f, 0.0f, 1);
            stack.mStackControlInfo.mNowShadowAlpha = value;
        }
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setScale(MiuiFreeFormActivityStack stack, float scale) {
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setScale value:" + scale + " stack:" + stack.getStackPackageName());
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (animalLock.mCurrentAnimation != 0 && animalLock.mCurrentAnimation != 2) {
            if (animalLock.mCurrentAnimation == 10) {
                float x = (this.mResizeLeftPosition + stack.mStackControlInfo.mScaleWindowWidth) - (stack.mStackControlInfo.mScaleWindowWidth * scale);
                float y = this.mResizeTopPosition;
                this.mListener.mGestureAnimator.setPositionInTransaction(stack, x, y);
                Slog.d(TAG, "setScale ANIMATION_RESIZE_BACK_LEFT_BOTTOM");
            } else if (animalLock.mCurrentAnimation == 11) {
                Slog.d(TAG, "setScale ANIMATION_RESIZE_BACK_RIGHT_BOTTOM");
            } else if (animalLock.mCurrentAnimation == 14) {
                float x2 = (this.mListener.mScreenWidth / 2.0f) - ((this.mListener.mScreenWidth * scale) / 2.0f);
                float y2 = (this.mListener.mScreenHeight / 2.0f) - ((this.mListener.mScreenHeight * scale) / 2.0f);
                this.mListener.mGestureAnimator.setPositionInTransaction(stack, x2, y2);
            }
        }
        this.mListener.mGestureAnimator.setMatrixInTransaction(stack, scale, 0.0f, 0.0f, scale);
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setXScale(MiuiFreeFormActivityStack stack, float scale) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setXScale value:" + scale + " stack:" + stack.getStackPackageName() + " animalLock:" + animalLock);
        }
        stack.mStackControlInfo.mNowWidthScale = scale;
        if (animalLock.mCurrentAnimation != 3 && animalLock.mCurrentAnimation != 4) {
            if (animalLock.mCurrentAnimation == 8) {
                if (MiuiFreeFormGestureController.DEBUG) {
                    Slog.d(TAG, "setXScale value:" + scale + " stack:" + stack.getStackPackageName() + " mScaleWindowWidth:" + stack.mStackControlInfo.mScaleWindowWidth + " currentTempX=" + stack.mStackControlInfo.mOriPosX);
                }
                stack.mStackControlInfo.mNowPosX = stack.mStackControlInfo.mOriPosX + ((stack.mStackControlInfo.mScaleWindowWidth * (1.0f - stack.mStackControlInfo.mNowWidthScale)) / 2.0f);
                this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
            } else if (animalLock.mCurrentAnimation == 15) {
                if (stack.mStackControlInfo.mOriPosX == this.mListener.mScreenWidth / 2) {
                    stack.mStackControlInfo.mNowPosX = stack.mStackControlInfo.mOriPosX - ((this.mListener.mScreenWidth * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
                    this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
                }
            } else if (animalLock.mCurrentAnimation == 20) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 25) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 30) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 26) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 27) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 28) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            } else if (animalLock.mCurrentAnimation == 29) {
                stack.mStackControlInfo.mNowWidthScale = scale;
            }
        }
        this.mListener.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mNowWidthScale, 0.0f, 0.0f, stack.mStackControlInfo.mNowHeightScale);
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
        Object obj = mLock;
        synchronized (obj) {
            if (animalLock.mCurrentAnimation == 12 && !this.mAlreadySetXScale) {
                this.mAlreadySetXScale = true;
                obj.notifyAll();
            }
        }
    }

    public void setYScale(MiuiFreeFormActivityStack stack, float scale) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setYScale value:" + scale + " stack:" + stack.getStackPackageName() + " animalLock:" + animalLock);
        }
        stack.mStackControlInfo.mNowHeightScale = scale;
        if (animalLock.mCurrentAnimation != 3 && animalLock.mCurrentAnimation != 4 && animalLock.mCurrentAnimation != 8) {
            if (animalLock.mCurrentAnimation == 15) {
                if (stack.mStackControlInfo.mOriPosY == this.mListener.mScreenHeight / 2) {
                    stack.mStackControlInfo.mNowPosY = stack.mStackControlInfo.mOriPosY - ((this.mListener.mScreenHeight * stack.mStackControlInfo.mNowHeightScale) / 2.0f);
                    this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
                }
            } else if (animalLock.mCurrentAnimation == 20) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 25) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 30) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 26) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 27) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 28) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            } else if (animalLock.mCurrentAnimation == 29) {
                stack.mStackControlInfo.mNowHeightScale = scale;
            }
        }
        this.mListener.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mNowWidthScale, 0.0f, 0.0f, stack.mStackControlInfo.mNowHeightScale);
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
        Object obj = mLock;
        synchronized (obj) {
            if (animalLock.mCurrentAnimation == 12 && !this.mAlreadySetYScale) {
                this.mAlreadySetYScale = true;
                obj.notifyAll();
            }
        }
    }

    public void drawIcon(SurfaceControl surfaceControl, MiuiFreeFormActivityStack mffas, float finalScaleX, float finalScaleY) {
        Surface surface = (Surface) this.mListener.mService.mSurfaceFactory.get();
        IMiuiScreenProjectionStub imp = IMiuiScreenProjectionStub.getInstance();
        if (surfaceControl != null && this.mListener.mService.isInScreenProjection()) {
            surfaceControl.setScreenProjection(imp.getExtraScreenProjectFlag());
        }
        surface.copyFrom(surfaceControl);
        try {
            Canvas canvas = surface.lockCanvas(null);
            if (canvas != null) {
                int color = this.mListener.mService.mContext.getColor(285605963);
                canvas.drawColor(color);
                Rect realBounds = new Rect();
                realBounds.set(mffas.mTask.getBounds());
                int i = realBounds.left;
                int j = realBounds.top;
                realBounds.scale(mffas.mFreeFormScale);
                realBounds.offsetTo(i, j);
                float scaleX = ((int) MiuiMultiWindowUtils.applyDip2Px(56.0f)) / (mffas.mExitIconWidth * finalScaleX);
                float scaleY = ((int) MiuiMultiWindowUtils.applyDip2Px(56.0f)) / (mffas.mExitIconHeight * finalScaleY);
                this.mTempMatrix.reset();
                this.mTempMatrix.setScale(scaleX, scaleY, 0.0f, 0.0f);
                this.mTempMatrix.postTranslate((realBounds.width() / 2) - ((mffas.mExitIconWidth / 2) * scaleX), (realBounds.height() / 2) - ((mffas.mExitIconHeight / 2) * scaleY));
                if (mffas.mExitIconBitmap != null) {
                    canvas.drawBitmap(mffas.mExitIconBitmap, this.mTempMatrix, this.mPaint);
                }
                surface.unlockCanvasAndPost(canvas);
            } else {
                Slog.e(TAG, "drawIcon canvas is null");
            }
        } catch (Surface.OutOfResourcesException e) {
            Slog.e(TAG, "drawIcon out of resource");
        } catch (IllegalArgumentException e2) {
            Slog.e(TAG, "drawIcon illegal argument");
        }
        surface.destroy();
    }

    public void setPositionX(MiuiFreeFormActivityStack stack, float x) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        float posX = x;
        if (animalLock.mCurrentAnimation == 8) {
            posX = x + ((stack.mStackControlInfo.mScaleWindowWidth * (1.0f - stack.mStackControlInfo.mNowWidthScale)) / 2.0f);
            stack.mStackControlInfo.mOriPosX = x;
        } else if (animalLock.mCurrentAnimation == 3) {
            posX = x;
        } else if (animalLock.mCurrentAnimation != 4) {
            if (animalLock.mCurrentAnimation == 6) {
                posX = x - ((this.mListener.mScreenWidth * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
            } else if (animalLock.mCurrentAnimation == 15) {
                stack.mStackControlInfo.mOriPosX = x;
                posX = x - ((this.mListener.mScreenWidth * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
            } else if (animalLock.mCurrentAnimation == 12) {
                if (!stack.mIsLandcapeFreeform) {
                    posX = x - ((this.mListener.mScreenWidth * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
                } else {
                    posX = x - ((this.mListener.mScreenHeight * stack.mStackControlInfo.mNowWidthScale) / 2.0f);
                }
            } else if (animalLock.mCurrentAnimation == 7) {
                synchronized (this.mListener.mService.mGlobalLock) {
                    float[] widthAndHeight = getSmallwindowWidthHeight(stack);
                    stack.mStackControlInfo.mSmallWindowBounds = new Rect(new Rect((int) stack.mStackControlInfo.mNowPosX, (int) stack.mStackControlInfo.mNowPosY, (int) (stack.mStackControlInfo.mNowPosX + widthAndHeight[0]), (int) (stack.mStackControlInfo.mNowPosY + widthAndHeight[1])));
                    SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
                    this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
                    inputTransaction.apply();
                }
                posX = x;
            } else if (animalLock.mCurrentAnimation == 9) {
                synchronized (this.mListener.mService.mGlobalLock) {
                    float[] widthAndHeight2 = getSmallwindowWidthHeight(stack);
                    stack.mStackControlInfo.mSmallWindowBounds = new Rect((int) x, (int) stack.mStackControlInfo.mNowPosY, (int) (widthAndHeight2[0] + x), (int) (stack.mStackControlInfo.mNowPosY + widthAndHeight2[1]));
                    SurfaceControl.Transaction inputTransaction2 = new SurfaceControl.Transaction();
                    this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction2);
                    inputTransaction2.apply();
                }
                posX = x;
            } else if (animalLock.mCurrentAnimation == 11 || animalLock.mCurrentAnimation == 10) {
                posX = x;
                this.mResizeLeft = x;
            } else if (animalLock.mCurrentAnimation == 20) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 25) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 30) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 26) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 27) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 28) {
                posX = x;
            } else if (animalLock.mCurrentAnimation == 29) {
                posX = x;
            } else {
                posX = x;
            }
        }
        WindowState flashBackTrafficWindow = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.mFlashBackTrafficWindow;
        WindowState flashBackActionWindow = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.mFlashBackActionWindow;
        if (flashBackTrafficWindow != null && stack.isInMiniFreeFormMode()) {
            this.mListener.mGestureAnimator.setPositionInTransaction(flashBackTrafficWindow.mToken, (posX - flashBackTrafficWindow.mWindowFrames.mFrame.left) - 1.0f, (((stack.mStackControlInfo.mNowPosY + stack.mStackControlInfo.mSmallWindowBounds.height()) - flashBackTrafficWindow.mWindowFrames.mFrame.top) - flashBackTrafficWindow.mWindowFrames.mFrame.height()) + 4.0f);
        }
        if (flashBackActionWindow != null && stack.isInMiniFreeFormMode()) {
            float height = stack.mStackControlInfo.mNowPosY + stack.mStackControlInfo.mSmallWindowBounds.height();
            MiuiFreeFormFlashBackHelper miuiFreeFormFlashBackHelper = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper;
            this.mListener.mGestureAnimator.setPositionInTransaction(flashBackActionWindow.mToken, (posX - flashBackActionWindow.mWindowFrames.mFrame.left) - 1.0f, (height + MiuiFreeFormFlashBackHelper.getFloatWindowMarginToSmallWindow(this.mListener.mService.mContext)) - flashBackActionWindow.mWindowFrames.mFrame.top);
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setPositionX posX:" + posX + " x:" + x + " currentTempX:" + stack.mStackControlInfo.mOriPosX + " PosY:" + stack.mStackControlInfo.mNowPosY + " animalLock:" + animalLock + " mCurrentAnimation:" + animalLock.mCurrentAnimation + " stack:" + stack.getStackPackageName());
        }
        stack.mStackControlInfo.mNowPosX = posX;
        this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void setPositionY(MiuiFreeFormActivityStack stack, float y) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        float posY = y;
        if (animalLock.mCurrentAnimation == 8) {
            posY = y;
            stack.mStackControlInfo.mOriPosY = y;
        } else if (animalLock.mCurrentAnimation == 3) {
            posY = y;
        } else if (animalLock.mCurrentAnimation != 4) {
            if (animalLock.mCurrentAnimation == 6) {
                posY = y - ((this.mListener.mScreenHeight * stack.mStackControlInfo.mNowHeightScale) / 2.0f);
            } else if (animalLock.mCurrentAnimation == 15) {
                stack.mStackControlInfo.mOriPosY = y;
                posY = y - ((this.mListener.mScreenHeight * stack.mStackControlInfo.mNowHeightScale) / 2.0f);
            } else if (animalLock.mCurrentAnimation == 12) {
                if (!stack.mIsLandcapeFreeform) {
                    posY = y - ((this.mListener.mScreenHeight * stack.mStackControlInfo.mNowHeightScale) / 2.0f);
                } else {
                    posY = y - ((this.mListener.mScreenWidth * stack.mStackControlInfo.mNowHeightScale) / 2.0f);
                }
                if (posY < MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController, false)) {
                    posY = MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController, false);
                }
            } else if (animalLock.mCurrentAnimation == 7) {
                float[] widthAndHeight = getSmallwindowWidthHeight(stack);
                synchronized (this.mListener.mService.mGlobalLock) {
                    stack.mStackControlInfo.mSmallWindowBounds = new Rect((int) stack.mStackControlInfo.mNowPosX, (int) y, (int) (stack.mStackControlInfo.mNowPosX + widthAndHeight[0]), (int) (widthAndHeight[1] + y));
                    SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
                    this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
                    inputTransaction.apply();
                }
                posY = y;
            } else if (animalLock.mCurrentAnimation == 9) {
                synchronized (this.mListener.mService.mGlobalLock) {
                    float[] widthAndHeight2 = getSmallwindowWidthHeight(stack);
                    stack.mStackControlInfo.mSmallWindowBounds = new Rect((int) stack.mStackControlInfo.mNowPosX, (int) y, (int) (stack.mStackControlInfo.mNowPosX + widthAndHeight2[0]), (int) (widthAndHeight2[1] + y));
                    SurfaceControl.Transaction inputTransaction2 = new SurfaceControl.Transaction();
                    this.mListener.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction2);
                    inputTransaction2.apply();
                }
                posY = y;
            } else if (animalLock.mCurrentAnimation == 11 || animalLock.mCurrentAnimation == 10) {
                posY = y;
                this.mResizeTop = y;
            } else if (animalLock.mCurrentAnimation == 20) {
                stack.mStackControlInfo.mNowPosY = y;
                posY = y;
            } else if (animalLock.mCurrentAnimation == 25) {
                posY = y;
            } else if (animalLock.mCurrentAnimation == 30) {
                posY = y;
            } else if (animalLock.mCurrentAnimation == 26) {
                posY = y;
            } else if (animalLock.mCurrentAnimation == 27) {
                posY = y;
            } else if (animalLock.mCurrentAnimation == 28) {
                posY = y;
            } else if (animalLock.mCurrentAnimation == 29) {
                posY = y;
            } else {
                posY = y;
            }
        }
        WindowState flashBackTrafficWindow = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.mFlashBackTrafficWindow;
        WindowState flashBackActionWindow = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.mFlashBackActionWindow;
        if (flashBackTrafficWindow != null && stack.isInMiniFreeFormMode()) {
            this.mListener.mGestureAnimator.setPositionInTransaction(flashBackTrafficWindow.mToken, (stack.mStackControlInfo.mNowPosX - flashBackTrafficWindow.mWindowFrames.mFrame.left) - 1.0f, (((stack.mStackControlInfo.mSmallWindowBounds.height() + posY) - flashBackTrafficWindow.mWindowFrames.mFrame.top) - flashBackTrafficWindow.mWindowFrames.mFrame.height()) + 4.0f);
        }
        if (flashBackActionWindow != null && stack.isInMiniFreeFormMode()) {
            MiuiFreeFormFlashBackHelper miuiFreeFormFlashBackHelper = this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper;
            this.mListener.mGestureAnimator.setPositionInTransaction(flashBackActionWindow.mToken, (stack.mStackControlInfo.mNowPosX - flashBackActionWindow.mWindowFrames.mFrame.left) - 1.0f, ((stack.mStackControlInfo.mSmallWindowBounds.height() + posY) + MiuiFreeFormFlashBackHelper.getFloatWindowMarginToSmallWindow(this.mListener.mService.mContext)) - flashBackActionWindow.mWindowFrames.mFrame.top);
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "setPositionY PosY:" + posY + " PosX:" + stack.mStackControlInfo.mNowPosX + " animalLock:" + animalLock + " mCurrentAnimation:" + animalLock.mCurrentAnimation + " stack:" + stack.getStackPackageName());
        }
        stack.mStackControlInfo.mNowPosY = posY;
        this.mListener.mGestureAnimator.setPositionInTransaction(stack, stack.mStackControlInfo.mNowPosX, stack.mStackControlInfo.mNowPosY);
        if (MiuiFreeFormAnimationHandler.getInstance().getMiuiFreeFormGestureAnimator() == null) {
            this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    /* JADX WARN: Incorrect condition in loop: B:68:0x020c */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void allAnimalFinishedActions(com.android.server.wm.MiuiFreeFormGestureAnimator.AnimalLock r29) {
        /*
            Method dump skipped, instructions count: 2142
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.allAnimalFinishedActions(com.android.server.wm.MiuiFreeFormGestureAnimator$AnimalLock):void");
    }

    void finishAnimationControl(MiuiFreeFormActivityStack stack, Rect bound, float scale, int mode) {
        this.mListener.mService.openSurfaceTransaction();
        if (scale > 0.0f && scale < 1.0f) {
            try {
                try {
                    stack.mFreeFormScale = scale;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Throwable th) {
                this.mListener.mService.closeSurfaceTransaction("FreeFormAnimationControl reset animation");
                throw th;
            }
        }
        this.mListener.mGestureAnimator.removeAnimationControlLeash(stack);
        this.mListener.mService.mActivityManager.resizeTask(stack.mStackID, bound, mode);
        this.mListener.mService.closeSurfaceTransaction("FreeFormAnimationControl reset animation");
        this.mListener.mService.scheduleAnimationLocked();
    }

    private void adjustFreeFormBoundsIfNeed(MiuiFreeFormActivityStack stack) {
        Rect visualBounds = new Rect(stack.mStackControlInfo.mWindowBounds.left, stack.mStackControlInfo.mWindowBounds.top, (int) (stack.mStackControlInfo.mWindowBounds.left + (stack.mStackControlInfo.mWindowBounds.width() * stack.mFreeFormScale)), (int) (stack.mStackControlInfo.mWindowBounds.top + (stack.mStackControlInfo.mWindowBounds.height() * stack.mFreeFormScale)));
        if (this.mListener.mFreeFormAccessibleArea.contains(visualBounds)) {
            return;
        }
        stack.mStackControlInfo.mWindowBounds.set(stack.mTask.getBounds());
        Slog.d(TAG, "adjust freeform bounds to" + stack.mStackControlInfo.mWindowBounds);
    }

    public void onAnimationEnd(MiuiFreeFormGestureAnimator.AnimalLock animalLock, MiuiFreeFormSpringAnimation dynamicAnimation) {
        animalLock.animationEnd(dynamicAnimation);
        Slog.d(TAG, "onAnimationEnd currentAnimation:" + animalLock.mCurrentAnimation + " isAnimalFinished:" + animalLock.isAnimalFinished());
        if (animalLock.isAnimalFinished()) {
            if (animalLock.mStack != null) {
                this.mStackLocks.remove(animalLock.mStack);
            }
            allAnimalFinishedActions(animalLock);
            animalLock.resetAnimalState();
        }
    }

    public float getOriFreeformLandscapeHeight(MiuiFreeFormActivityStack stack) {
        return MiuiMultiWindowUtils.getPossibleBounds(this.mListener.mService.mContext, false, false, stack.getStackPackageName()).height() * MiuiMultiWindowUtils.getOriFreeformScale(this.mListener.mService.mContext, false);
    }

    public float[] getSmallwindowWidthHeight(MiuiFreeFormActivityStack stack) {
        float[] widthAndHeight = new float[2];
        if (stack.isLaunchedByCVW() && !stack.mCVWControlInfo.miniFreeformBounds.isEmpty()) {
            widthAndHeight[0] = stack.mCVWControlInfo.miniFreeformBounds.width();
            widthAndHeight[1] = stack.mCVWControlInfo.miniFreeformBounds.height();
        } else {
            RectF smallFreeformRect = MiuiMultiWindowUtils.getSmallFreeformRect(this.mListener.mService.mContext, stack.mIsLandcapeFreeform);
            widthAndHeight[0] = smallFreeformRect.width();
            widthAndHeight[1] = smallFreeformRect.height();
        }
        return widthAndHeight;
    }

    public void onRoundCornorAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onRoundCornorAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onClipWidthXAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onClipWidthXAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onClipHeightYAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onClipHeightYAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onShadowAlphaAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onShadowAlphaAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onAlphaAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onAlphaAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onScaleAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onScaleAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onScaleXAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onScaleXAnimationEnd\u3000animalLock:" + animalLock + " dynamicAnimation:" + dynamicAnimation);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onScaleYAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onScaleYAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onTranslateYAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onTranslateYAnimationEnd\u3000animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    public void onTranslateXAnimationEnd(MiuiFreeFormSpringAnimation dynamicAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) dynamicAnimation.mTarget;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mStackLocks.get(stack);
        if (animalLock == null) {
            return;
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onTranslateXAnimationEnd animalLock:" + animalLock);
        }
        onAnimationEnd(animalLock, dynamicAnimation);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(17:61|4|(1:6)(1:7)|8|(12:13|15|16|57|17|(2:55|19)|22|23|59|(6:63|25|26|33|(1:35)|36)(1:39)|40|45)|14|15|16|57|17|(0)|22|23|59|(0)(0)|40|45) */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x0137, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x0139, code lost:
        r0 = e;
     */
    /* JADX WARN: Removed duplicated region for block: B:39:0x010f A[Catch: Exception -> 0x0137, all -> 0x0159, TryCatch #3 {Exception -> 0x0137, blocks: (B:32:0x00dc, B:33:0x00f1, B:35:0x00f7, B:36:0x00fd, B:39:0x010f, B:40:0x011a), top: B:59:0x00c1 }] */
    /* JADX WARN: Removed duplicated region for block: B:55:0x00a0 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:63:0x00c3 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void showScreenSurface(final com.android.server.wm.MiuiFreeFormActivityStack r19) {
        /*
            Method dump skipped, instructions count: 352
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormWindowMotionHelper.showScreenSurface(com.android.server.wm.MiuiFreeFormActivityStack):void");
    }

    public void hideScreenSurface(MiuiFreeFormActivityStack stack) {
        synchronized (this.mSurfaceControlLock) {
            Slog.d(TAG, "hideScreenSurface:" + this.mSurfaceControl);
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null && surfaceControl.isValid()) {
                SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mListener.mService.mTransactionFactory.get();
                t.remove(this.mSurfaceControl).apply();
                this.mSurfaceControl = null;
            }
        }
    }

    public void setOriginalBounds(Rect rect, MiuiFreeFormActivityStack stack) {
        this.mOriginalBounds = rect;
        this.mListener.mGestureController.adjustFreeformStackOrientation(stack);
    }

    /* loaded from: classes.dex */
    private final class AnimalFinishedActionsRunnable implements Runnable {
        WeakReference<MiuiFreeFormGestureAnimator.AnimalLock> mAnimalLock;

        AnimalFinishedActionsRunnable(WeakReference<MiuiFreeFormGestureAnimator.AnimalLock> animalLock) {
            MiuiFreeFormWindowMotionHelper.this = r1;
            this.mAnimalLock = animalLock;
        }

        @Override // java.lang.Runnable
        public void run() {
            MiuiFreeFormWindowMotionHelper.this.allAnimalFinishedActions(this.mAnimalLock.get());
        }
    }

    private void setRotation(SurfaceControl.Transaction t, int rotation, int width, int height) {
        Matrix snapshotInitialMatrix = new Matrix();
        int delta = RotationUtils.deltaRotation(rotation, 0);
        RotationAnimationUtils.createRotationMatrix(delta, width, height, snapshotInitialMatrix);
        setSnapshotTransform(t, snapshotInitialMatrix, 1.0f);
    }

    private void setSnapshotTransform(SurfaceControl.Transaction t, Matrix matrix, float alpha) {
        SurfaceControl surfaceControl = this.mSurfaceControl;
        if (surfaceControl != null && surfaceControl.isValid()) {
            float[] mTmpFloats = new float[9];
            matrix.getValues(mTmpFloats);
            float x = mTmpFloats[2];
            float y = mTmpFloats[5];
            t.setPosition(this.mSurfaceControl, x, y);
            t.setMatrix(this.mSurfaceControl, mTmpFloats[0], mTmpFloats[3], mTmpFloats[1], mTmpFloats[4]);
            t.setAlpha(this.mSurfaceControl, alpha);
            t.show(this.mSurfaceControl);
        }
    }

    public static void createRotationMatrix(int rotation, int width, int height, Matrix outMatrix) {
        switch (rotation) {
            case 0:
                outMatrix.reset();
                return;
            case 1:
                outMatrix.setRotate(90.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(height, 0.0f);
                return;
            case 2:
                outMatrix.setRotate(180.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(width, height);
                return;
            case 3:
                outMatrix.setRotate(270.0f, 0.0f, 0.0f);
                outMatrix.postTranslate(0.0f, width);
                return;
            default:
                return;
        }
    }

    public void setRequestedOrientation(int requestedOrientation, MiuiFreeFormActivityStack stack, boolean noAnimation) {
        int top;
        int left;
        int left2;
        int top2;
        if (requestedOrientation == -1 || requestedOrientation == 13 || requestedOrientation == 10 || requestedOrientation == 4 || requestedOrientation == 2 || requestedOrientation == -2 || requestedOrientation == 14) {
            Slog.d(TAG, "ignore requested orientation");
            return;
        }
        Rect rect = stack.mTask.getBounds();
        stack.mIsLandcapeFreeform = rect.width() > rect.height();
        Slog.d(TAG, "setRequestedOrientation stack = " + stack.getStackPackageName() + " requestedOrientation= " + requestedOrientation + " isLandcapeFreeform= " + stack.mIsLandcapeFreeform + " isOrientationLandscape = " + MiuiMultiWindowUtils.isOrientationLandscape(requestedOrientation));
        if (MiuiMultiWindowUtils.isOrientationLandscape(requestedOrientation) == stack.mIsLandcapeFreeform) {
            return;
        }
        if (stack.mStackControlInfo.mCurrentAnimation != 28 && stack.mStackControlInfo.mCurrentAnimation != 29) {
            cancelAllSpringAnimal();
            Context context = stack.mTask.mAtmService.mContext;
            WindowManager wm = (WindowManager) context.getSystemService("window");
            int statusBarHeight = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(this.mListener.mGestureController.mDisplayContent.mDisplayFrames));
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(outMetrics);
            int displayWidth = outMetrics.widthPixels;
            int displayHeight = outMetrics.heightPixels;
            float freeScale = stack.mFreeFormScale;
            int heightCenter = rect.top + ((int) (((rect.height() * freeScale) / 2.0f) + 0.5f));
            int widthCenter = rect.left + ((int) (((rect.width() * freeScale) / 2.0f) + 0.5f));
            boolean isVertical = outMetrics.heightPixels > outMetrics.widthPixels;
            boolean isLandscapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(requestedOrientation);
            RectF possibleBounds = MiuiMultiWindowUtils.getPossibleBounds(context, isVertical, isLandscapeFreeform, stack.getStackPackageName());
            float scale = MiuiMultiWindowUtils.getOriFreeformScale(context, isLandscapeFreeform);
            float widthOri = possibleBounds.width();
            float heightOri = possibleBounds.height();
            float widthAfterScale = possibleBounds.width() * scale;
            float heightAfterScale = possibleBounds.height() * scale;
            if (isLandscapeFreeform) {
                int left3 = (int) ((widthCenter - (widthAfterScale / 2.0f)) + 0.5f);
                if (left3 + widthAfterScale <= displayWidth) {
                    left = left3;
                } else {
                    left = (int) ((displayWidth - widthAfterScale) + 0.5f);
                }
                if (isVertical) {
                    if (left < 0) {
                        left = 0;
                    }
                    top = (int) ((heightCenter - (heightAfterScale / 2.0f)) + 0.5f);
                    if (top < statusBarHeight) {
                        top = statusBarHeight;
                    }
                } else {
                    if (left < statusBarHeight) {
                        left = statusBarHeight;
                    }
                    top = (int) ((heightCenter - (heightAfterScale / 2.0f)) + 0.5f);
                }
            } else {
                int top3 = (int) ((heightCenter - (heightAfterScale / 2.0f)) + 0.5f);
                if (isVertical) {
                    left2 = (int) ((widthCenter - (widthAfterScale / 2.0f)) + 0.5f);
                } else {
                    left2 = (int) ((widthCenter - (widthAfterScale / 2.0f)) + 0.5f);
                    if (left2 < statusBarHeight) {
                        left2 = statusBarHeight;
                    }
                }
                if (top3 + heightAfterScale <= displayHeight) {
                    top2 = top3;
                } else {
                    top2 = (int) ((displayHeight - heightAfterScale) + 0.5f);
                }
                if (top2 >= statusBarHeight) {
                    int i = left2;
                    top = top2;
                    left = i;
                } else {
                    int i2 = left2;
                    top = statusBarHeight;
                    left = i2;
                }
            }
            int right = (int) (left + widthOri + 0.5f);
            int bottom = (int) (top + heightOri + 0.5f);
            Rect setBounds = new Rect(left, top, right, bottom);
            Slog.d(TAG, "setRequestedOrientation rect = " + setBounds);
            stack.mIsLandcapeFreeform = setBounds.width() > setBounds.height();
            if (noAnimation) {
                stack.mFreeFormScale = scale;
                try {
                    this.mListener.mService.mActivityManager.resizeTask(stack.mStackID, setBounds, 2);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            stack.mStackControlInfo.mCurrentAnimation = 21;
            applyAlphaHideAnimation(stack, setBounds, scale);
            return;
        }
        Slog.d(TAG, "skip setRequestedOrientation stack = " + stack.getStackPackageName() + " stack.mStackControlInfo.mCurrentAnimation= " + stack.mStackControlInfo.mCurrentAnimation);
    }

    /* renamed from: com.android.server.wm.MiuiFreeFormWindowMotionHelper$4 */
    /* loaded from: classes.dex */
    public class AnonymousClass4 implements Runnable {
        final /* synthetic */ float val$scale;
        final /* synthetic */ Rect val$setBounds;
        final /* synthetic */ MiuiFreeFormActivityStack val$stack;

        AnonymousClass4(MiuiFreeFormActivityStack miuiFreeFormActivityStack, Rect rect, float f) {
            MiuiFreeFormWindowMotionHelper.this = this$0;
            this.val$stack = miuiFreeFormActivityStack;
            this.val$setBounds = rect;
            this.val$scale = f;
        }

        @Override // java.lang.Runnable
        public void run() {
            Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaHideAnimation stack= " + this.val$stack + " setBounds= " + this.val$setBounds + " scale= " + this.val$scale);
            synchronized (MiuiFreeFormWindowMotionHelper.this.mListener.mService.mGlobalLock) {
                if (this.val$stack.getFreeFormConrolSurface() == null) {
                    MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.createLeash(this.val$stack);
                }
            }
            ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
            animator.setDuration(300L);
            final MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.val$stack;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$4$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    MiuiFreeFormWindowMotionHelper.AnonymousClass4.this.m1750x95ddf0a5(miuiFreeFormActivityStack, valueAnimator);
                }
            });
            animator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper.4.1
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaHideAnimation onAnimationStart ");
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaHideAnimation onAnimationEnd ");
                    AnonymousClass4.this.val$stack.mFreeFormScale = AnonymousClass4.this.val$scale;
                    try {
                        MiuiFreeFormWindowMotionHelper.this.mListener.mService.mActivityManager.resizeTask(AnonymousClass4.this.val$stack.mStackID, AnonymousClass4.this.val$setBounds, 2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    MiuiFreeFormWindowMotionHelper.this.applyAlphaShowAnimation(AnonymousClass4.this.val$stack);
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaHideAnimation onAnimationCancel ");
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                }
            });
            animator.start();
        }

        /* renamed from: lambda$run$0$com-android-server-wm-MiuiFreeFormWindowMotionHelper$4 */
        public /* synthetic */ void m1750x95ddf0a5(MiuiFreeFormActivityStack stack, ValueAnimator valueAnimator) {
            float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setAlphaInTransaction(stack, value);
            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.applyTransaction();
        }
    }

    public void applyAlphaHideAnimation(MiuiFreeFormActivityStack stack, Rect setBounds, float scale) {
        this.mListener.mGestureController.mHandler.post(new AnonymousClass4(stack, setBounds, scale));
    }

    public void applyAlphaShowAnimation(MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "applyAlphaShowAnimation stack= " + stack + " stack.mTask.getBounds()= " + stack.mTask.getBounds());
        Rect rect = stack.mTask.getBounds();
        this.mListener.mGestureController.mHandler.postDelayed(new AnonymousClass5(stack, rect), 200L);
    }

    /* renamed from: com.android.server.wm.MiuiFreeFormWindowMotionHelper$5 */
    /* loaded from: classes.dex */
    public class AnonymousClass5 implements Runnable {
        final /* synthetic */ Rect val$rect;
        final /* synthetic */ MiuiFreeFormActivityStack val$stack;

        AnonymousClass5(MiuiFreeFormActivityStack miuiFreeFormActivityStack, Rect rect) {
            MiuiFreeFormWindowMotionHelper.this = this$0;
            this.val$stack = miuiFreeFormActivityStack;
            this.val$rect = rect;
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (MiuiFreeFormWindowMotionHelper.this.mListener.mService.mGlobalLock) {
                if (this.val$stack.getFreeFormConrolSurface() == null) {
                    MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.createLeash(this.val$stack);
                }
            }
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setDuration(300L);
            final MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.val$stack;
            final Rect rect = this.val$rect;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper$5$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    MiuiFreeFormWindowMotionHelper.AnonymousClass5.this.m1751x95ddf0a6(miuiFreeFormActivityStack, rect, valueAnimator);
                }
            });
            animator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormWindowMotionHelper.5.1
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaShowAnimation onAnimationStart ");
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaShowAnimation onAnimationEnd ");
                    MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.removeAnimationControlLeash(AnonymousClass5.this.val$stack);
                    AnonymousClass5.this.val$stack.mStackControlInfo.mCurrentAnimation = -1;
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    Slog.d(MiuiFreeFormWindowMotionHelper.TAG, "applyAlphaShowAnimation onAnimationCancel ");
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                }
            });
            animator.start();
        }

        /* renamed from: lambda$run$0$com-android-server-wm-MiuiFreeFormWindowMotionHelper$5 */
        public /* synthetic */ void m1751x95ddf0a6(MiuiFreeFormActivityStack stack, Rect rect, ValueAnimator valueAnimator) {
            float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setPositionInTransaction(stack, rect.left, rect.top);
            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.setAlphaInTransaction(stack, value);
            MiuiFreeFormWindowMotionHelper.this.mListener.mGestureAnimator.applyTransaction();
        }
    }
}
