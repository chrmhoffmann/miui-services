package com.android.server.wm;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.MiuiMultiWindowUtils;
import android.util.TypedValue;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.widget.Toast;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwPolicy;
import com.android.server.wm.MiuiCvwSnapTargetPool;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import miui.app.MiuiFreeFormManager;
import miui.os.Build;
import miui.util.HapticFeedbackUtil;
/* loaded from: classes.dex */
public class MiuiCvwGestureController implements MiuiCvwGestureControllerStub {
    static final String CVW_INPUT_CONSUMER = "cvw_input_consumer";
    static final int FLAG_GESTURE_LEFT = 4;
    static final int FLAG_GESTURE_RIGHT = 8;
    public static final int HOT_SPACE_BOTTOM_CAPTION = 1;
    public static final int HOT_SPACE_CVW_LEFT_RESIZE = 4;
    public static final int HOT_SPACE_CVW_RIGHT_RESIZE = 5;
    public static final int HOT_SPACE_LEFT_RESIZE_REGION = 2;
    public static final int HOT_SPACE_RESIZE_CENTER_HORIZONTAL_MARGIN = 33;
    public static final int HOT_SPACE_RESIZE_CENTER_VERTICAL_MARGIN = 33;
    public static final int HOT_SPACE_RESIZE_CENTRAL_MARGIN_PAD = 24;
    public static final int HOT_SPACE_RESIZE_HEIGHT = 132;
    public static final int HOT_SPACE_RESIZE_OFFSET_PAD = 33;
    public static final int HOT_SPACE_RESIZE_RADIUS = 50;
    public static final int HOT_SPACE_RESIZE_WIDTH = 132;
    public static final int HOT_SPACE_RIGHT_RESIZE_REGION = 3;
    public static final int HOT_SPACE_TOP_CAPTION = 0;
    private static final int TIME_MAX_CVW_TOUCH = 800;
    private static final int TIME_MIN_CVW_TOUCH = 150;
    private static final float VALID_TOUCH_RATIO = 0.075f;
    private long mCurDownTime;
    private float mCurDownX;
    private float mCurDownY;
    private WindowState mCurrScreenWindowState;
    MiuiCvwPolicy mCvwPolicy;
    MiuiCvwGestureHandlerImpl mDefaultGestureHandler;
    DisplayContent mDisplayContent;
    private MotionEvent mDownEvent;
    HandlerThread mExecuteThread;
    private volatile int mGestureFlag;
    GesturePointerEventListener mGesturePointerListener;
    H mHandler;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    boolean mInFreeformRegion;
    boolean mInHotArea;
    MiuiFreeFormManagerService mMiuiFreeFormManagerService;
    RootWindowContainer mRootWindowContainer;
    MiuiFreeFormActivityStack mStack;
    MiuiCvwTaskOrganizer mTaskOrganizer;
    WindowManagerService mWmService;
    private boolean needFreezeHomeAppTransitionOnce;
    private static final String TAG = MiuiCvwGestureController.class.getSimpleName();
    public static boolean IS_TABLET = "tablet".equals(SystemProperties.get("ro.build.characteristics", ""));
    private static final int MAX_HOT_AREA_TOP_OFFSET = (int) TypedValue.applyDimension(1, 32.0f, Resources.getSystem().getDisplayMetrics());
    private static int mDeltaHotArea = 0;
    private boolean mWindowAllDrawn = false;
    private int mTaskSupportCvw = -1;
    Toast mToast = null;
    private volatile boolean mGestureCanceled = false;
    boolean isFreeformResizingByCvw = false;
    private boolean mPerformedHapticFeedback = false;
    private final Rect mBottomLeftCorner = new Rect();
    private final Rect mBottomRightCorner = new Rect();
    private final MiuiMultiWindowUtils.RoundedRectF mLeftResizeRegion = new MiuiMultiWindowUtils.RoundedRectF();
    private final MiuiMultiWindowUtils.RoundedRectF mRightResizeRegion = new MiuiMultiWindowUtils.RoundedRectF();
    CvwTrackEvent mTrackEvent = new CvwTrackEvent();
    private final Runnable removeLayerRunnable = new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureController.1
        @Override // java.lang.Runnable
        public void run() {
            Slog.d(MiuiCvwGestureController.TAG, "removeLayerRunnable mWindowAllDrawn = " + MiuiCvwGestureController.this.mWindowAllDrawn);
            MiuiCvwGestureController.this.windowDrawFinished();
            MiuiCvwGestureController.this.mDefaultGestureHandler.tryRemoveOverlays(0L);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiCvwGestureController> {

        /* compiled from: MiuiCvwGestureController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiCvwGestureController INSTANCE = new MiuiCvwGestureController();
        }

        public MiuiCvwGestureController provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiCvwGestureController provideNewInstance() {
            return new MiuiCvwGestureController();
        }
    }

    public void init(WindowManagerService service) {
        Slog.d(TAG, "MiuiCvwGestureController init");
        this.mWmService = service;
        RootWindowContainer rootWindowContainer = service.mRoot;
        this.mRootWindowContainer = rootWindowContainer;
        this.mDisplayContent = rootWindowContainer.getDefaultDisplay();
        HandlerThread handlerThread = new HandlerThread("cvw_execute_thread", -4);
        this.mExecuteThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(this.mExecuteThread.getLooper());
        this.mCvwPolicy = new MiuiCvwPolicy(this);
        GesturePointerEventListener gesturePointerEventListener = new GesturePointerEventListener();
        this.mGesturePointerListener = gesturePointerEventListener;
        this.mWmService.registerPointerEventListener(gesturePointerEventListener, 0);
        this.mDefaultGestureHandler = new MiuiCvwGestureHandlerImpl(this);
        this.mTaskOrganizer = new MiuiCvwTaskOrganizer(this);
        this.mMiuiFreeFormManagerService = (MiuiFreeFormManagerService) service.mAtmService.mMiuiFreeFormManagerService;
    }

    public static boolean isMiuiCvwFeatureEnable() {
        return IS_TABLET;
    }

    public void onDisplayInfoChanged(final DisplayContent displayContent, final Configuration config) {
        this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureController$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwGestureController.this.m1551xe99382c0(displayContent, config);
            }
        });
    }

    /* renamed from: lambda$onDisplayInfoChanged$0$com-android-server-wm-MiuiCvwGestureController */
    public /* synthetic */ void m1551xe99382c0(DisplayContent displayContent, Configuration config) {
        MiuiCvwPolicy miuiCvwPolicy;
        if (displayContent.getDisplayId() == 0 && (miuiCvwPolicy = this.mCvwPolicy) != null) {
            miuiCvwPolicy.onDisplayInfoChanged(displayContent, config);
            this.mDefaultGestureHandler.updateScreenParams(displayContent, config);
        }
    }

    public void onTaskVanished(Task task) {
        Slog.d(TAG, "onTaskVanished: " + task);
        MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mStack;
        if (miuiFreeFormActivityStack != null && miuiFreeFormActivityStack.mTask == task) {
            this.mDefaultGestureHandler.onTaskVanished(task);
        }
    }

    public boolean isFreeformResizingByCvw() {
        boolean temp = this.isFreeformResizingByCvw;
        this.isFreeformResizingByCvw = false;
        Slog.d(TAG, "isFreeformResizingByCvw " + temp);
        return temp;
    }

    public void freezeHomeAppTransitionOnce() {
        this.needFreezeHomeAppTransitionOnce = true;
    }

    public boolean needFreezeHomeAppTransitionOnce(WindowContainer activityRecord) {
        if (activityRecord instanceof ActivityRecord) {
            boolean temp = this.needFreezeHomeAppTransitionOnce;
            this.needFreezeHomeAppTransitionOnce = false;
            return temp && InputMethodManagerServiceImpl.MIUI_HOME.equals(((ActivityRecord) activityRecord).packageName);
        }
        return false;
    }

    public boolean supportPortraitRatioInConfig(String packageName) {
        return MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(packageName, 1) || MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(packageName, 2);
    }

    public boolean supportLandscapeRatioInConfig(String packageName) {
        return MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(packageName, 3);
    }

    public void resetGestureAnimatorState() {
        Slog.d(TAG, "resetGestureAnimatorState");
        if (!Debug.isDebuggerConnected()) {
            this.mHandler.removeCallbacks(this.removeLayerRunnable);
            this.mHandler.postDelayed(this.removeLayerRunnable, 2000L);
        }
    }

    public int getGestureFlag() {
        return this.mGestureFlag;
    }

    public void setMagicSplitBarAndWallpaperVisibility(Task task, boolean visibility) {
    }

    public void cancelCVWGesture() {
        this.mGestureCanceled = true;
    }

    public boolean gestureCanceled() {
        return this.mGestureCanceled;
    }

    public void showToast() {
        Toast toast = this.mToast;
        if (toast != null) {
            toast.cancel();
        }
        Toast makeText = Toast.makeText(this.mWmService.mContext, this.mWmService.mContext.getString(286196348), 0);
        this.mToast = makeText;
        makeText.show();
    }

    public boolean isTaskSupportCvw(Task task) {
        if (this.mTaskSupportCvw == -1 && task != null) {
            this.mTaskSupportCvw = this.mWmService.mAtmService.getTaskResizeableForFreeform(task.mTaskId) ? 1 : 0;
        }
        if (task != null && task.getWindowingMode() == 5) {
            this.mTaskSupportCvw = 1;
        }
        return this.mTaskSupportCvw == 1;
    }

    public MiuiCvwSnapTargetPool.SnapTarget findSnapTargetByRatio(float ratio) {
        return this.mCvwPolicy.getSnapTargetByRatio(ratio);
    }

    public void notifyFreeFormApplicationResizeStart() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureController$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwGestureController.this.m1550x3f70c5ab();
            }
        });
    }

    /* renamed from: lambda$notifyFreeFormApplicationResizeStart$1$com-android-server-wm-MiuiCvwGestureController */
    public /* synthetic */ void m1550x3f70c5ab() {
        if (this.mMiuiFreeFormManagerService != null && this.mStack != null) {
            Slog.d(TAG, MiuiFreeFormManager.actionToString(6));
            this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(6, this.mStack);
        }
    }

    public void notifyFreeFormApplicationResizeEnd() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureController$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwGestureController.this.m1549xf4cadbb1();
            }
        });
    }

    /* renamed from: lambda$notifyFreeFormApplicationResizeEnd$2$com-android-server-wm-MiuiCvwGestureController */
    public /* synthetic */ void m1549xf4cadbb1() {
        if (this.mMiuiFreeFormManagerService != null && this.mStack != null) {
            Slog.d(TAG, MiuiFreeFormManager.actionToString(7));
            this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(7, this.mStack);
        }
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args != null && args.length > 1) {
            String next = args[1];
            if ("0".equals(next)) {
                Slog.closeLog();
                return;
            } else if (SplitScreenReporter.ACTION_ENTER_SPLIT.equals(next)) {
                Slog.openLog();
                return;
            } else {
                pw.println("unknown cmd " + next);
                return;
            }
        }
        pw.println("MiuiCvwGestureController:");
        pw.print("  ");
        pw.println("CurDownPoint(" + this.mCurDownX + ", " + this.mCurDownY + ")");
        pw.print("  ");
        pw.println("GestureFlag:" + this.mGestureFlag);
        pw.print("  ");
        StringBuilder append = new StringBuilder().append("mTaskId:");
        MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mStack;
        pw.println(append.append(miuiFreeFormActivityStack == null ? "invalid" : Integer.valueOf(miuiFreeFormActivityStack.mStackID)).toString());
        pw.print("  ");
        StringBuilder append2 = new StringBuilder().append("mTask:");
        MiuiFreeFormActivityStack miuiFreeFormActivityStack2 = this.mStack;
        pw.println(append2.append((Object) (miuiFreeFormActivityStack2 == null ? "null" : miuiFreeFormActivityStack2.mTask)).toString());
        pw.print("  ");
        pw.println("mInFreeformRegion:" + this.mInFreeformRegion);
        pw.print("  ");
        pw.println("mInHotArea:" + this.mInHotArea);
        pw.print("  ");
        pw.println("mBottomLeftCorner:" + this.mBottomLeftCorner);
        pw.print("  ");
        pw.println("mBottomRightCorner:" + this.mBottomRightCorner);
        pw.print("  ");
        pw.println("mLeftResizeRegion:" + this.mLeftResizeRegion);
        pw.print("  ");
        pw.println("mRightResizeRegion:" + this.mRightResizeRegion);
        pw.print("  ");
        pw.println("mWindowAllDrawn:" + this.mWindowAllDrawn);
        pw.print("  ");
        pw.println("isFreeformResizingByCvw:" + this.isFreeformResizingByCvw);
        pw.print("  ");
        pw.println("needFreezeHomeAppTransitionOnce:" + this.needFreezeHomeAppTransitionOnce);
        pw.print("  ");
        pw.println("mTaskSupportCvw:" + this.mTaskSupportCvw);
        if (this.mCurrScreenWindowState != null) {
            pw.print("  ");
            pw.println("mCurrScreenWindowState mCvwGestureAnimating :" + this.mCurrScreenWindowState.mCvwGestureAnimating);
        }
        MiuiCvwGestureHandlerImpl miuiCvwGestureHandlerImpl = this.mDefaultGestureHandler;
        if (miuiCvwGestureHandlerImpl != null) {
            miuiCvwGestureHandlerImpl.dump(pw, "");
        }
        MiuiCvwPolicy miuiCvwPolicy = this.mCvwPolicy;
        if (miuiCvwPolicy != null) {
            miuiCvwPolicy.dump(pw, "");
        }
    }

    /* loaded from: classes.dex */
    public class GesturePointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        private static final int EVENT_SEND_TO_HANDWRITING = 134217728;
        private InputEventReceiver mInputEventReceiver;
        private final IBinder mToken;

        private GesturePointerEventListener() {
            MiuiCvwGestureController.this = r1;
            this.mToken = new Binder();
        }

        public void onPointerEvent(MotionEvent event) {
            if (!event.isTouchEvent()) {
                return;
            }
            if ((event.getFlags() & 134217728) == 134217728) {
                if (event.getActionMasked() == 0) {
                    Slog.d(MiuiCvwGestureController.TAG, "Because handwriting, drop this event");
                }
            } else if (MiuiMultiWindowUtils.isUserAMonkey()) {
                Slog.d(MiuiCvwGestureController.TAG, "MTBF Or Monkey is Running");
            } else if (MiuiCvwGestureController.this.mWmService.isKeyguardLocked()) {
                Slog.d(MiuiCvwGestureController.TAG, "Keyguard was Locked");
            } else if (!MiuiMultiWindowUtils.supportFreeform()) {
                Slog.d(MiuiCvwGestureController.TAG, "This device does not support freeform mode");
            } else {
                int curAction = event.getAction();
                if (curAction != 0 && MiuiCvwGestureController.this.mDownEvent == null) {
                    return;
                }
                float x = event.getRawX();
                float y = event.getRawY();
                switch (curAction) {
                    case 0:
                        if (MiuiCvwGestureController.this.mDefaultGestureHandler.isGestureAnimating()) {
                            Slog.d(MiuiCvwGestureController.TAG, "isGestureAnimating");
                            if (MiuiCvwGestureController.this.mStack == null) {
                                Slog.d(MiuiCvwGestureController.TAG, "isGestureAnimating=true but mStack=null");
                                MiuiCvwGestureController.this.removeLayerRunnable.run();
                                return;
                            }
                            return;
                        } else if (isWithinFullScreenNavRegion(x, y)) {
                            Slog.d(MiuiCvwGestureController.TAG, "touch in FullScreenNavRegion x=" + x + ",y=" + y);
                            return;
                        } else {
                            MiuiCvwGestureController.this.mGestureFlag = 0;
                            MiuiCvwGestureController.this.mGestureCanceled = false;
                            MiuiCvwGestureController.this.mCurDownX = event.getRawX();
                            MiuiCvwGestureController.this.mCurDownY = event.getRawY();
                            MiuiCvwGestureController.this.mCurDownTime = System.currentTimeMillis();
                            if (!isWithinOverlayWindowRegion((int) MiuiCvwGestureController.this.mCurDownX, (int) MiuiCvwGestureController.this.mCurDownY)) {
                                MiuiCvwGestureController miuiCvwGestureController = MiuiCvwGestureController.this;
                                miuiCvwGestureController.mInFreeformRegion = isWithinFreeformRegion((int) miuiCvwGestureController.mCurDownX, (int) MiuiCvwGestureController.this.mCurDownY);
                                if (!MiuiCvwGestureController.this.mInFreeformRegion) {
                                    MiuiCvwGestureController miuiCvwGestureController2 = MiuiCvwGestureController.this;
                                    miuiCvwGestureController2.mInHotArea = isWithinHotAreaRegion((int) miuiCvwGestureController2.mCurDownX, (int) MiuiCvwGestureController.this.mCurDownY);
                                }
                            }
                            Slog.d(MiuiCvwGestureController.TAG, "mInFreeformRegion :" + MiuiCvwGestureController.this.mInFreeformRegion + " mInHotArea:" + MiuiCvwGestureController.this.mInHotArea + " mCurDownX:" + MiuiCvwGestureController.this.mCurDownX + " mCurDownY:" + MiuiCvwGestureController.this.mCurDownY + ", mGestureFlag:" + MiuiCvwGestureController.this.mGestureFlag);
                            MiuiCvwGestureController.this.mDownEvent = MotionEvent.obtain(event);
                            if (MiuiCvwGestureController.this.mInFreeformRegion || MiuiCvwGestureController.this.mInHotArea) {
                                MiuiCvwGestureController.this.mHandler.removeCallbacks(MiuiCvwGestureController.this.removeLayerRunnable);
                                MiuiCvwGestureController.this.mDefaultGestureHandler.onTouchEvent(MiuiCvwGestureController.this.mDownEvent);
                                return;
                            }
                            return;
                        }
                    case 1:
                    case 3:
                        if (MiuiCvwGestureController.this.mGestureFlag != 0 && !MiuiCvwGestureController.this.mDefaultGestureHandler.isResizeFinished()) {
                            MiuiCvwGestureController.this.mDefaultGestureHandler.onTouchEvent(event);
                        }
                        resetTouchStateImmediately();
                        return;
                    case 2:
                        if (!MiuiCvwGestureController.this.mInHotArea && !MiuiCvwGestureController.this.mInFreeformRegion) {
                            return;
                        }
                        if (MiuiCvwGestureController.this.gestureCanceled()) {
                            Slog.d(MiuiCvwGestureController.TAG, "Gesture has been canceled, return!");
                            return;
                        } else if (MiuiCvwGestureController.this.mDefaultGestureHandler.isResizeFinished()) {
                            Slog.d(MiuiCvwGestureController.TAG, "isGestureAnimating");
                            return;
                        } else {
                            if (MiuiCvwGestureController.this.mInHotArea) {
                                if (MiuiCvwGestureController.this.mStack != null) {
                                    MiuiCvwGestureController miuiCvwGestureController3 = MiuiCvwGestureController.this;
                                    if (miuiCvwGestureController3.isTaskSupportCvw(miuiCvwGestureController3.mStack.mTask) && System.currentTimeMillis() - MiuiCvwGestureController.this.mCurDownTime < 150) {
                                        return;
                                    }
                                }
                                double distance = Math.hypot(Math.abs(x - MiuiCvwGestureController.this.mCurDownX), Math.abs(y - MiuiCvwGestureController.this.mCurDownY));
                                if (distance < MiuiCvwGestureController.this.mCvwPolicy.getTouchSlop()) {
                                    if (System.currentTimeMillis() - MiuiCvwGestureController.this.mCurDownTime > 800) {
                                        MiuiCvwGestureController.this.mInHotArea = false;
                                        return;
                                    }
                                    return;
                                }
                            }
                            if (MiuiCvwGestureController.this.mGestureFlag != 0) {
                                performHapticFeedback();
                                MiuiCvwGestureController.this.mDefaultGestureHandler.onTouchEvent(event);
                                return;
                            }
                            return;
                        }
                    default:
                        return;
                }
            }
        }

        private boolean isWithinOverlayWindowRegion(final int x, final int y) {
            boolean z;
            synchronized (MiuiCvwGestureController.this.mWmService.mAtmService.mGlobalLock) {
                final AtomicBoolean isTouchOverlayWindow = new AtomicBoolean(false);
                MiuiCvwGestureController.this.mDisplayContent.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiCvwGestureController$GesturePointerEventListener$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        MiuiCvwGestureController.GesturePointerEventListener.this.m1554x750e8db4(x, y, isTouchOverlayWindow, (WindowState) obj);
                    }
                }, true);
                z = isTouchOverlayWindow.get();
            }
            return z;
        }

        /* renamed from: lambda$isWithinOverlayWindowRegion$0$com-android-server-wm-MiuiCvwGestureController$GesturePointerEventListener */
        public /* synthetic */ void m1554x750e8db4(int x, int y, AtomicBoolean isTouchOverlayWindow, WindowState w) {
            if (w == null) {
                return;
            }
            boolean isTouchable = (w.mAttrs.flags & 16) == 0 && (w.mAttrs.inputFeatures & 1) == 0;
            if (!isTouchable || (w.mAttrs.privateFlags & 1048576) != 0) {
                return;
            }
            int type = w.mAttrs.type;
            if (WindowManager.LayoutParams.isSystemAlertWindowType(type)) {
                return;
            }
            int layer = MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(type);
            int statusBarLayer = MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(2000);
            if (layer <= statusBarLayer || !w.isVisibleNow()) {
                return;
            }
            Region region = new Region();
            if (layer == MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(2020) || layer == MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(2017) || layer == MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(2040) || layer == MiuiCvwGestureController.this.mWmService.mPolicy.getWindowLayerFromTypeLw(2024)) {
                w.getTouchableRegion(region);
                if (region.contains(x, y)) {
                    Slog.d(MiuiCvwGestureController.TAG, "isWithinOverlayWindowRegion exist overlay window " + w);
                    isTouchOverlayWindow.set(true);
                }
                Slog.d(MiuiCvwGestureController.TAG, "isWithinOverlayWindowRegion " + w.isVisibleNow() + ", " + w + ", type = " + type);
            }
        }

        private boolean isWithinFullScreenNavRegion(float x, float y) {
            return MiuiMultiWindowUtils.isInFullScreenNavHotArea(MiuiCvwGestureController.this.mWmService.mContext, x, y);
        }

        private boolean isWithinFreeformRegion(int x, int y) {
            boolean result = false;
            MiuiFreeFormActivityStack freeFormStack = null;
            if (MiuiCvwGestureController.this.mWmService.mAtmService.mMiuiFreeFormManagerService != null && (freeFormStack = MiuiCvwGestureController.this.mMiuiFreeFormManagerService.getTargetResizeMiuiFreeFormStack(x, y)) != null && freeFormStack.checkReadyForCvwControl()) {
                Slog.d(MiuiCvwGestureController.TAG, "isWithinFreeformRegion freeFormStack.mStackRatio :" + freeFormStack.mStackRatio);
                MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = MiuiCvwGestureController.this.mCvwPolicy.updateFreeformTaskWrapperInfo(freeFormStack);
                int taskId = freeFormStack.mStackID;
                Rect currentCvwBounds = new Rect();
                if (freeFormStack.isInMiniFreeFormMode() || freeFormStack.isInFreeFormMode()) {
                    currentCvwBounds.set(taskWrapperInfo.visualBounds);
                }
                Task task = freeFormStack.mTask;
                if (task == null || currentCvwBounds.isEmpty()) {
                    Slog.d(MiuiCvwGestureController.TAG, "taskId:" + taskId + " bounds null or empty or not in freeform!");
                    return false;
                }
                MiuiMultiWindowUtils.getCvwResizeRegions(MiuiCvwGestureController.this.mLeftResizeRegion, MiuiCvwGestureController.this.mRightResizeRegion, currentCvwBounds);
                Slog.d(MiuiCvwGestureController.TAG, "taskId:" + taskId + " currentCvwBounds:" + currentCvwBounds + " mLeftResizeRegion:" + MiuiCvwGestureController.this.mLeftResizeRegion + " mRightResizeRegion:" + MiuiCvwGestureController.this.mRightResizeRegion + " task:" + task);
                if (MiuiCvwGestureController.this.mLeftResizeRegion.contains(x, y)) {
                    MiuiCvwGestureController.this.mGestureFlag &= -9;
                    MiuiCvwGestureController.this.mGestureFlag |= 4;
                    result = true;
                }
                if (MiuiCvwGestureController.this.mRightResizeRegion.contains(x, y)) {
                    MiuiCvwGestureController.this.mGestureFlag &= -5;
                    MiuiCvwGestureController.this.mGestureFlag |= 8;
                    result = true;
                }
            }
            if (result) {
                MiuiCvwGestureController.this.mStack = freeFormStack;
                ActivityRecord topRunning = MiuiCvwGestureController.this.mStack.mTask.topRunningActivityLocked();
                if (topRunning != null) {
                    MiuiCvwGestureController.this.mCurrScreenWindowState = topRunning.findMainWindow();
                }
            }
            return result;
        }

        private boolean isWithinHotAreaRegion(final int x, final int y) {
            boolean z;
            if (!MiuiMultiWindowUtils.isFullScreenGestureNav(MiuiCvwGestureController.this.mWmService.mContext) || Build.IS_INTERNATIONAL_BUILD) {
                Slog.d(MiuiCvwGestureController.TAG, "isWithinHotAreaRegion FullScreenNav noSupport ");
                return false;
            }
            final int navBarHeight = MiuiCvwGestureController.this.mCvwPolicy.getNavBarHeight();
            synchronized (MiuiCvwGestureController.this.mWmService.mAtmService.mGlobalLock) {
                final boolean[] inHotArea = {false};
                try {
                    MiuiCvwGestureController.this.mDisplayContent.getDefaultTaskDisplayArea().forAllTasks(new Consumer() { // from class: com.android.server.wm.MiuiCvwGestureController$GesturePointerEventListener$$ExternalSyntheticLambda0
                        @Override // java.util.function.Consumer
                        public final void accept(Object obj) {
                            MiuiCvwGestureController.GesturePointerEventListener.this.m1553xd6b78c39(inHotArea, x, y, navBarHeight, (Task) obj);
                        }
                    });
                } catch (Exception e) {
                    Slog.w(MiuiCvwGestureController.TAG, "isWithinHotAreaRegion catch :" + e.getMessage());
                }
                z = inHotArea[0];
            }
            return z;
        }

        /* renamed from: lambda$isWithinHotAreaRegion$1$com-android-server-wm-MiuiCvwGestureController$GesturePointerEventListener */
        public /* synthetic */ void m1553xd6b78c39(boolean[] inHotArea, int x, int y, int navBarHeight, Task task) {
            if (task.isVisible() && !inHotArea[0]) {
                inHotArea[0] = isWithinHotAreaRegion(task, x, y, navBarHeight);
                Slog.d(MiuiCvwGestureController.TAG, "isWithinHotAreaRegion " + task.isVisible() + ", " + task + ", " + inHotArea[0]);
            }
        }

        private boolean isWithinHotAreaRegion(Task task, int x, int y, int navBarHeight) {
            WindowState mainWindowState;
            if (task == null || task.isActivityTypeHome()) {
                Slog.d(MiuiCvwGestureController.TAG, "touch hot area, did not get a right stack.");
                return false;
            } else if (task.mSurfaceAnimator.isAnimating() && task.mSurfaceAnimator.getAnimationType() == 8) {
                Slog.d(MiuiCvwGestureController.TAG, "doing recent task animation,return.");
                return false;
            } else if (MiuiCvwGestureController.this.inFreeform(task)) {
                Slog.d(MiuiCvwGestureController.TAG, "No processing in freeform window mode.");
                return false;
            } else {
                ActivityRecord topRunning = task.topRunningActivityLocked();
                if (topRunning == null || !topRunning.firstWindowDrawn || (mainWindowState = topRunning.findMainWindow()) == null) {
                    return false;
                }
                Rect hotAreaBounds = MiuiCvwGestureController.this.mCvwPolicy.getHotAreaBounds();
                if (hotAreaBounds == null || hotAreaBounds.isEmpty()) {
                    Slog.d(MiuiCvwGestureController.TAG, "HotAreaBounds null or empty!");
                    return false;
                }
                Slog.d(MiuiCvwGestureController.TAG, "hotAreaBounds:" + hotAreaBounds + " topRunning bounds :" + topRunning.getBounds() + ", " + topRunning.getWindowingMode());
                if (hotAreaBounds.width() != topRunning.getBounds().width() && !MiuiCvwGestureController.this.isActivityEmbedded(topRunning) && !topRunning.isLetterboxedForFixedOrientationAndAspectRatio()) {
                    Slog.d(MiuiCvwGestureController.TAG, "top task not fullscreen!");
                    return false;
                }
                int hotAreaTopOffset = Math.min(MiuiCvwGestureController.MAX_HOT_AREA_TOP_OFFSET, navBarHeight * 2);
                if (hotAreaTopOffset == 0) {
                    hotAreaTopOffset = MiuiCvwGestureController.MAX_HOT_AREA_TOP_OFFSET;
                }
                MiuiCvwGestureController.mDeltaHotArea = (int) (MiuiCvwPolicy.mCurrScreenWidth * MiuiCvwGestureController.VALID_TOUCH_RATIO);
                MiuiCvwGestureController.this.mBottomLeftCorner.set(hotAreaBounds.left, hotAreaBounds.bottom - hotAreaTopOffset, hotAreaBounds.left + MiuiCvwGestureController.mDeltaHotArea, hotAreaBounds.bottom);
                MiuiCvwGestureController.this.mBottomRightCorner.set(hotAreaBounds.right - MiuiCvwGestureController.mDeltaHotArea, hotAreaBounds.bottom - hotAreaTopOffset, hotAreaBounds.right, hotAreaBounds.bottom);
                Slog.d(MiuiCvwGestureController.TAG, "hotAreaBounds:" + hotAreaBounds + " mBottomLeftCorner:" + MiuiCvwGestureController.this.mBottomLeftCorner + ", " + MiuiCvwGestureController.this.mBottomRightCorner + ", mDeltaHotArea = " + MiuiCvwGestureController.mDeltaHotArea);
                boolean result = false;
                if (!hotAreaContains(MiuiCvwGestureController.this.mBottomLeftCorner, x, y)) {
                    if (hotAreaContains(MiuiCvwGestureController.this.mBottomRightCorner, x, y)) {
                        MiuiCvwGestureController.this.mGestureFlag &= -5;
                        MiuiCvwGestureController miuiCvwGestureController = MiuiCvwGestureController.this;
                        miuiCvwGestureController.mGestureFlag = 8 | miuiCvwGestureController.mGestureFlag;
                        result = true;
                    }
                } else {
                    MiuiCvwGestureController.this.mGestureFlag &= -9;
                    MiuiCvwGestureController.this.mGestureFlag |= 4;
                    result = true;
                }
                if (result && MiuiCvwGestureController.this.mCvwPolicy.isForbiddenWindow(topRunning)) {
                    Slog.d(MiuiCvwGestureController.TAG, "the activity inHotArea not support cvw :" + topRunning.packageName);
                    MiuiCvwGestureController.this.mGestureFlag = 0;
                    throw new ForceReturn("hotArea activity not support cvw, return!!");
                }
                if (result) {
                    MiuiCvwGestureController.this.mStack = new MiuiFreeFormActivityStack(task);
                    MiuiCvwGestureController.this.mStack.mStackControlInfo.mWindowBounds = new Rect(hotAreaBounds);
                    MiuiCvwGestureController.this.mStack.setStackFreeFormMode(-1);
                    MiuiCvwGestureController.this.mCvwPolicy.updateFullScreenTaskWrapperInfo(MiuiCvwGestureController.this.mStack);
                    MiuiCvwGestureController.this.mCurrScreenWindowState = mainWindowState;
                }
                return result;
            }
        }

        private boolean hotAreaContains(Rect bounds, int x, int y) {
            return bounds.left < bounds.right && bounds.top < bounds.bottom && x >= bounds.left && x <= bounds.right && y >= bounds.top && y <= bounds.bottom;
        }

        private void performHapticFeedback() {
            if (!MiuiCvwGestureController.this.mPerformedHapticFeedback && MiuiCvwGestureController.this.mInHotArea) {
                if (MiuiCvwGestureController.this.mHapticFeedbackUtil == null) {
                    MiuiCvwGestureController.this.mHapticFeedbackUtil = new HapticFeedbackUtil(MiuiCvwGestureController.this.mWmService.mContext, false);
                }
                MiuiCvwGestureController.this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_down", false);
                MiuiCvwGestureController.this.mPerformedHapticFeedback = true;
            }
        }

        public void registerInputConsumer() {
            Slog.d(MiuiCvwGestureController.TAG, "registerInputConsumer");
            if (this.mInputEventReceiver == null) {
                synchronized (MiuiCvwGestureController.this.mWmService.mAtmService.mGlobalLock) {
                    InputChannel inputChanel = new InputChannel();
                    int displayId = MiuiCvwGestureController.this.mDisplayContent.getDisplayId();
                    try {
                        MiuiCvwGestureController.this.mWmService.destroyInputConsumer(MiuiCvwGestureController.CVW_INPUT_CONSUMER, displayId);
                        MiuiCvwGestureController.this.mWmService.createInputConsumer(this.mToken, MiuiCvwGestureController.CVW_INPUT_CONSUMER, displayId, inputChanel);
                        this.mInputEventReceiver = new InputEventReceiver(inputChanel, Looper.getMainLooper()) { // from class: com.android.server.wm.MiuiCvwGestureController.GesturePointerEventListener.1
                            public void onInputEvent(InputEvent event) {
                                super.onInputEvent(event);
                            }
                        };
                        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
                        MiuiCvwGestureController.this.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
                        inputTransaction.apply();
                    } catch (Exception e) {
                    }
                }
            }
        }

        private void unregisterInputConsumer() {
            Slog.d(MiuiCvwGestureController.TAG, "unregisterInputConsumer");
            try {
                if (this.mInputEventReceiver != null) {
                    synchronized (MiuiCvwGestureController.this.mWmService.mAtmService.mGlobalLock) {
                        int displayId = MiuiCvwGestureController.this.mDisplayContent.getDisplayId();
                        MiuiCvwGestureController.this.mWmService.destroyInputConsumer(MiuiCvwGestureController.CVW_INPUT_CONSUMER, displayId);
                        this.mInputEventReceiver.dispose();
                        this.mInputEventReceiver = null;
                    }
                }
            } catch (Exception e) {
                this.mInputEventReceiver = null;
            }
        }

        private void resetTouchStateImmediately() {
            MiuiCvwGestureController miuiCvwGestureController = MiuiCvwGestureController.this;
            miuiCvwGestureController.mCurDownY = -1.0f;
            miuiCvwGestureController.mCurDownX = -1.0f;
            MiuiCvwGestureController miuiCvwGestureController2 = MiuiCvwGestureController.this;
            miuiCvwGestureController2.mInHotArea = false;
            miuiCvwGestureController2.mInFreeformRegion = false;
            MiuiCvwGestureController.this.mGestureFlag = 0;
            MiuiCvwGestureController.this.mTaskSupportCvw = -1;
            if (MiuiCvwGestureController.this.mDownEvent != null) {
                MiuiCvwGestureController.this.mDownEvent.recycle();
                MiuiCvwGestureController.this.mDownEvent = null;
            }
            MiuiCvwGestureController.this.mPerformedHapticFeedback = false;
            MiuiCvwGestureController.this.mGesturePointerListener.unregisterInputConsumer();
        }

        /* loaded from: classes.dex */
        public class ForceReturn extends RuntimeException {
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            ForceReturn(String message) {
                super(message);
                GesturePointerEventListener.this = r1;
            }
        }
    }

    public void startGesture() {
        WindowState windowState = this.mCurrScreenWindowState;
        if (windowState != null) {
            windowState.setCvwGestureAnimating(true);
        }
        if (this.mInHotArea) {
            this.mGesturePointerListener.registerInputConsumer();
        }
    }

    public void resetStateUi() {
        WindowState windowState = this.mCurrScreenWindowState;
        if (windowState != null) {
            windowState.setCvwGestureAnimating(false);
            this.mCurrScreenWindowState = null;
        }
        this.mStack = null;
        windowDrawFinished();
    }

    public void finishDrawingWindow(WindowState windowState) {
        if (this.mStack == null) {
            return;
        }
        String str = TAG;
        Slog.d(str, "finishDrawingWindow windowState:" + windowState + ", isActivelyRemove ：" + this.mDefaultGestureHandler.isActivelyRemove());
        if (this.mDefaultGestureHandler.isActivelyRemove()) {
            return;
        }
        try {
            WindowState topWindow = this.mStack.mTask.getTopActivity(false, true).findMainWindow();
            if (windowState == topWindow) {
                windowDrawFinished();
                long delay = 0;
                if (this.mDefaultGestureHandler.launchFreeformFromFullscreen()) {
                    delay = 50;
                }
                if (this.mDefaultGestureHandler.launchFullscreenFromFreeform()) {
                    delay = 500;
                }
                Slog.d(str, "windowDrawFinished :" + windowState + ",delay ：" + delay);
                this.mDefaultGestureHandler.tryRemoveOverlays(delay);
            } else if (stackIsAbnormal(this.mStack)) {
                this.mDefaultGestureHandler.tryRemoveOverlays(0L);
            }
        } catch (Exception e) {
        }
    }

    public void migrateToNewSurfaceControl(Task task, SurfaceControl.Transaction t) {
        try {
            SurfaceAnimator taskSurfaceAnimator = task.mSurfaceAnimator;
            if (taskSurfaceAnimator.isAnimating() && taskSurfaceAnimator.getAnimationType() == 1073741824) {
                if (taskSurfaceAnimator.hasLeash() && this.mDefaultGestureHandler.isGestureAnimating() && this.mStack.mTask.isTaskId(task.mTaskId)) {
                    SurfaceControl leash = taskSurfaceAnimator.mLeash;
                    t.reparent(this.mStack.mTask.getSurfaceControl(), leash);
                }
                return;
            }
            Slog.d(TAG, "The animation that is playing is not cvw animation");
        } catch (Exception e) {
        }
    }

    public void windowDrawBegin() {
        this.mWindowAllDrawn = false;
    }

    protected void windowDrawFinished() {
        this.mWindowAllDrawn = true;
    }

    public boolean isWindowAllDrawn() {
        return this.mWindowAllDrawn;
    }

    public boolean inFreeform(Task task) {
        return task != null && task.getWindowingMode() == 5;
    }

    boolean stackIsAbnormal(MiuiFreeFormActivityStack stack) {
        synchronized (this.mWmService.mAtmService.mGlobalLock) {
            if (stack != null) {
                if (stack.mTask != null) {
                    int childCount = stack.mTask.getChildCount();
                    Slog.d(TAG, "stackIsAbnormal  childCount " + childCount);
                    int finishingCount = 0;
                    for (int activityNdx = childCount - 1; activityNdx >= 0; activityNdx--) {
                        ActivityRecord r = stack.mTask.getChildAt(activityNdx).asActivityRecord();
                        if (r.app == null) {
                            windowDrawFinished();
                            return true;
                        }
                        if (r.finishing) {
                            finishingCount++;
                        }
                    }
                    if (finishingCount == childCount) {
                        Slog.d(TAG, "stackIsAbnormal  windowDrawFinished ");
                        windowDrawFinished();
                        return true;
                    }
                    return false;
                }
            }
            windowDrawFinished();
            return true;
        }
    }

    public boolean isActivityEmbedded(ActivityRecord activity) {
        if (activity == null) {
            return false;
        }
        return MiuiEmbeddingWindowServiceStub.get().isActivityEmbedded(activity, true);
    }

    /* loaded from: classes.dex */
    public static class H extends Handler {
        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    /* loaded from: classes.dex */
    public static class Slog {
        private static boolean VERBOSE = false;
        private static boolean DEBUG = false;
        private static boolean INFO = false;

        Slog() {
        }

        static void openLog() {
            VERBOSE = true;
            DEBUG = true;
            INFO = true;
        }

        static void closeLog() {
            VERBOSE = false;
            DEBUG = false;
            INFO = false;
        }

        public static boolean isDebugable() {
            return DEBUG;
        }

        static void v(String tag, String msg) {
            if (VERBOSE) {
                android.util.Slog.v(tag, msg);
            }
        }

        static void v(String tag, String msg, Throwable tr) {
            if (VERBOSE) {
                android.util.Slog.v(tag, msg, tr);
            }
        }

        public static void d(String tag, String msg) {
            if (DEBUG) {
                android.util.Slog.d(tag, msg);
            }
        }

        public static void d(String tag, String msg, Throwable tr) {
            if (DEBUG) {
                android.util.Slog.d(tag, msg, tr);
            }
        }

        public static void d(String tag, String msg, boolean isDevVersion) {
            if (DEBUG || isDevVersion) {
                android.util.Slog.d(tag, msg);
            }
        }

        public static void i(String tag, String msg) {
            if (INFO) {
                android.util.Slog.i(tag, msg);
            }
        }

        static void i(String tag, String msg, Throwable tr) {
            if (INFO) {
                android.util.Slog.i(tag, msg, tr);
            }
        }

        public static void w(String tag, String msg) {
            android.util.Slog.w(tag, msg);
        }

        static void w(String tag, String msg, Throwable tr) {
            android.util.Slog.w(tag, msg, tr);
        }

        static void w(String tag, Throwable tr) {
            android.util.Slog.w(tag, tr);
        }

        public static void e(String tag, String msg) {
            android.util.Slog.e(tag, msg);
        }

        public static void e(String tag, String msg, Throwable tr) {
            android.util.Slog.e(tag, msg, tr);
        }
    }

    /* loaded from: classes.dex */
    public static class CvwTrackEvent {
        public String originalStackRatio = "";
        public String targetStackRatio = "";
        public String enterWay = "";

        public void reset() {
            this.originalStackRatio = "";
            this.targetStackRatio = "";
            this.enterWay = "";
        }
    }

    /* loaded from: classes.dex */
    public interface CvwGestureHandler {
        void dump(PrintWriter printWriter, String str);

        boolean onTouchEvent(MotionEvent motionEvent);

        default boolean isGestureAnimating() {
            return false;
        }

        default boolean isResizeFinished() {
            return false;
        }
    }
}
