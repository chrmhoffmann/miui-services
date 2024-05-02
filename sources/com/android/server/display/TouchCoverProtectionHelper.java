package com.android.server.display;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.hardware.devicestate.DeviceStateManager;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Slog;
import android.view.DisplayCutout;
import android.view.DisplayInfo;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.widget.LinearLayout;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class TouchCoverProtectionHelper {
    private static boolean DEBUG = false;
    private static final int GAME_TOUCH_EVENT_INTERVAL = 300000;
    private static final String TAG = TouchCoverProtectionHelper.class.getSimpleName();
    private Rect mBorderRect;
    private Context mContext;
    private Rect mCurrentBorderRect;
    private DeviceStateManager mDeviceStateManager;
    private boolean mDisplayListenerEnabled;
    private DisplayManager mDisplayManager;
    private DisplayManagerServiceImpl mDisplayManagerServiceImpl;
    private DeviceStateManager.FoldStateListener mFoldStateListener;
    private Handler mHandler;
    private LogicalDisplay mLogicalDisplay;
    private int mLogicalHeight;
    private int mLogicalWidth;
    private int mRotation;
    private Rect mSecondDisplayBorderRect;
    private Object mSyncRoot;
    private boolean mTouchAreaEnabled;
    private int mTouchEventDebounce;
    private boolean mTouchTrackingEnabled;
    private final Rect mTouchCoverProtectionRect = new Rect();
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.display.TouchCoverProtectionHelper.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId != 0) {
                return;
            }
            TouchCoverProtectionHelper.this.updateTouchCoverProtectionRect();
        }
    };
    private Injector mInjector = new Injector();
    private WindowManagerService mWindowManagerService = ServiceManager.getService("window");
    private TouchPositionTracker mTouchPositionTracker = new TouchPositionTracker();

    public TouchCoverProtectionHelper(Context context, Looper looper) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        DisplayManagerServiceImpl displayManagerServiceImpl = (DisplayManagerServiceImpl) DisplayManagerServiceStub.getInstance();
        this.mDisplayManagerServiceImpl = displayManagerServiceImpl;
        this.mSyncRoot = displayManagerServiceImpl.getSyncRoot();
        Resources resources = context.getResources();
        this.mTouchAreaEnabled = resources.getBoolean(285540445);
        this.mBorderRect = setBorderRect(resources.getIntArray(285409328));
        this.mSecondDisplayBorderRect = setBorderRect(resources.getIntArray(285409324));
        this.mTouchEventDebounce = resources.getInteger(285933580);
        this.mCurrentBorderRect = this.mBorderRect;
        this.mFoldStateListener = new DeviceStateManager.FoldStateListener(context, new Consumer() { // from class: com.android.server.display.TouchCoverProtectionHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                TouchCoverProtectionHelper.this.m719xc8973b95((Boolean) obj);
            }
        });
        DeviceStateManager deviceStateManager = (DeviceStateManager) context.getSystemService(DeviceStateManager.class);
        this.mDeviceStateManager = deviceStateManager;
        deviceStateManager.registerCallback(new HandlerExecutor(this.mHandler), this.mFoldStateListener);
    }

    public void configure(boolean enable) {
        if (this.mTouchAreaEnabled) {
            setTouchTrackingEnabled(enable);
            setDisplayListenerEnabled(enable);
        }
    }

    public void setUpLogicalDisplay(LogicalDisplay logicalDisplay) {
        this.mLogicalDisplay = logicalDisplay;
        updateTouchCoverProtectionRect();
    }

    /* renamed from: updateBorderRect */
    public void m719xc8973b95(Boolean folded) {
        if (folded != null && folded.booleanValue()) {
            this.mCurrentBorderRect = this.mSecondDisplayBorderRect;
        } else {
            this.mCurrentBorderRect = this.mBorderRect;
        }
    }

    public void stop() {
        if (this.mTouchAreaEnabled) {
            setTouchTrackingEnabled(false);
            setDisplayListenerEnabled(false);
            showTouchCoverProtectionRect(false);
        }
        this.mDeviceStateManager.unregisterCallback(this.mFoldStateListener);
    }

    public void updateTouchCoverProtectionRect() {
        if (!this.mTouchAreaEnabled || this.mLogicalDisplay == null) {
            return;
        }
        synchronized (this.mSyncRoot) {
            DisplayDevice displayDevice = this.mLogicalDisplay.getPrimaryDisplayDeviceLocked();
            if (displayDevice == null) {
                Slog.w(TAG, "current display device is not exist");
                return;
            }
            DisplayDeviceInfo displayDeviceInfo = displayDevice.getDisplayDeviceInfoLocked();
            float physicalWidth = displayDeviceInfo.width;
            float physicalHeight = displayDeviceInfo.height;
            DisplayInfo displayInfo = this.mLogicalDisplay.getDisplayInfoLocked();
            int logicalWidth = displayInfo.logicalWidth;
            int logicalHeight = displayInfo.logicalHeight;
            int rotation = displayInfo.rotation;
            if (this.mLogicalWidth == logicalWidth && this.mLogicalHeight == logicalHeight && rotation == this.mRotation) {
                return;
            }
            this.mLogicalWidth = logicalWidth;
            this.mLogicalHeight = logicalHeight;
            this.mRotation = rotation;
            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;
            boolean z = true;
            if (rotation == 1) {
                left = this.mCurrentBorderRect.top;
                right = this.mCurrentBorderRect.bottom;
                top = (int) (physicalWidth - this.mCurrentBorderRect.right);
                bottom = (int) (physicalWidth - this.mCurrentBorderRect.left);
            } else if (rotation == 3) {
                left = (int) (physicalHeight - this.mCurrentBorderRect.bottom);
                right = (int) (physicalHeight - this.mCurrentBorderRect.top);
                top = this.mCurrentBorderRect.left;
                bottom = this.mCurrentBorderRect.right;
            }
            this.mTouchCoverProtectionRect.set(left, top, right, bottom);
            if (rotation != 1 && rotation != 3) {
                z = false;
            }
            boolean rotated = z;
            float scale = (rotated ? logicalHeight : logicalWidth) / physicalWidth;
            if (scale != 1.0d) {
                this.mTouchCoverProtectionRect.scale(scale);
            }
            Slog.d(TAG, "updateTouchCoverProtectionRect, mLogicalWidth: " + this.mLogicalWidth + ", mLogicalHeight: " + this.mLogicalHeight + ", physicalWidth: " + physicalWidth + ", physicalHeight: " + physicalHeight + ", scale: " + scale);
        }
    }

    public boolean isTouchCoverProtectionActive() {
        if (this.mTouchPositionTracker.isTouchingInArea()) {
            return true;
        }
        long now = SystemClock.uptimeMillis();
        if (now - this.mTouchPositionTracker.mLastObservedTouchTime < this.mTouchEventDebounce * 1000) {
            if (DEBUG) {
                Slog.d(TAG, "Time of light sensor event is within given time of touch event.");
            }
            return true;
        }
        return false;
    }

    public boolean isGameSceneWithinTouchTime() {
        long now = SystemClock.uptimeMillis();
        if (now - this.mTouchPositionTracker.mLastObservedTouchTime <= 300000) {
            if (DEBUG) {
                Slog.d(TAG, "Time of light sensor event is within given time of touch event in game scene.");
                return true;
            }
            return true;
        }
        return false;
    }

    private void setDisplayListenerEnabled(boolean enable) {
        if (enable) {
            if (!this.mDisplayListenerEnabled) {
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
                this.mDisplayListenerEnabled = true;
            }
        } else if (this.mDisplayListenerEnabled) {
            this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            this.mDisplayListenerEnabled = false;
        }
    }

    private Rect setBorderRect(int[] configArray) {
        if (configArray.length != 4) {
            Slog.w(TAG, "The touch cover array configuration must be four.");
            return new Rect(0, 0, 0, 0);
        }
        return new Rect(configArray[0], configArray[1], configArray[2], configArray[3]);
    }

    private void setTouchTrackingEnabled(boolean enable) {
        if (enable) {
            if (!this.mTouchTrackingEnabled) {
                this.mWindowManagerService.registerPointerEventListener(this.mTouchPositionTracker, 0);
                this.mTouchTrackingEnabled = true;
            }
        } else if (this.mTouchTrackingEnabled) {
            this.mWindowManagerService.unregisterPointerEventListener(this.mTouchPositionTracker, 0);
            this.mTouchTrackingEnabled = false;
        }
    }

    public void showTouchCoverProtectionRect(boolean isShow) {
        if (isShow) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.TouchCoverProtectionHelper$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TouchCoverProtectionHelper.this.m720x1628bfa2();
                }
            });
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.TouchCoverProtectionHelper$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    TouchCoverProtectionHelper.this.m721x3f7d14e3();
                }
            });
        }
    }

    /* renamed from: lambda$showTouchCoverProtectionRect$1$com-android-server-display-TouchCoverProtectionHelper */
    public /* synthetic */ void m720x1628bfa2() {
        this.mInjector.show();
    }

    /* renamed from: lambda$showTouchCoverProtectionRect$2$com-android-server-display-TouchCoverProtectionHelper */
    public /* synthetic */ void m721x3f7d14e3() {
        this.mInjector.hide();
    }

    public void dump(PrintWriter pw) {
        DEBUG = DisplayDebugConfig.DEBUG_ABC;
        pw.println("Touch cover protection state:");
        pw.println("  mTouchAreaEnabled=" + this.mTouchAreaEnabled);
        pw.println("  isTouchCoverProtectionActive=" + this.mTouchPositionTracker.isTouchingInArea());
        pw.println("  mBorderLeft=" + this.mCurrentBorderRect.left);
        pw.println("  mBorderTop=" + this.mCurrentBorderRect.top);
        pw.println("  mBorderRight=" + this.mCurrentBorderRect.right);
        pw.println("  mBorderBottom=" + this.mCurrentBorderRect.bottom);
        pw.println("  mTouchCoverProtectionRect=" + this.mTouchCoverProtectionRect);
        pw.println("  mTouchEventDebounce=" + this.mTouchEventDebounce);
    }

    /* loaded from: classes.dex */
    public final class TouchPositionTracker implements WindowManagerPolicyConstants.PointerEventListener {
        private volatile boolean mIsTouchingInArea;
        private volatile long mLastObservedTouchTime;
        private int mPointerIndexTriggerBitMask;

        private TouchPositionTracker() {
            TouchCoverProtectionHelper.this = r1;
        }

        public void onPointerEvent(MotionEvent motionEvent) {
            if (motionEvent.isTouchEvent()) {
                int action = motionEvent.getAction();
                switch (action & 255) {
                    case 0:
                    case 2:
                    case 5:
                        updateTouchStatus(motionEvent);
                        break;
                    case 1:
                    case 3:
                        this.mPointerIndexTriggerBitMask = 0;
                        break;
                    case 6:
                        int index = (65280 & action) >> 8;
                        int pointerId = motionEvent.getPointerId(index);
                        this.mPointerIndexTriggerBitMask &= ~(1 << pointerId);
                        break;
                }
                if (this.mPointerIndexTriggerBitMask != 0) {
                    this.mIsTouchingInArea = true;
                    this.mLastObservedTouchTime = SystemClock.uptimeMillis();
                    if (TouchCoverProtectionHelper.DEBUG) {
                        Slog.d(TouchCoverProtectionHelper.TAG, "onPointerEvent: touch events occurred in area");
                        return;
                    }
                    return;
                }
                this.mIsTouchingInArea = false;
            }
        }

        private void updateTouchStatus(MotionEvent motionEvent) {
            int pointerCount = motionEvent.getPointerCount();
            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            for (int i = 0; i < pointerCount; i++) {
                motionEvent.getPointerCoords(i, pointerCoords);
                int pointerId = motionEvent.getPointerId(i);
                float x = pointerCoords.getAxisValue(0);
                float y = pointerCoords.getAxisValue(1);
                if (!TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.contains(Math.round(x), Math.round(y))) {
                    this.mPointerIndexTriggerBitMask = (~(1 << pointerId)) & this.mPointerIndexTriggerBitMask;
                } else {
                    this.mPointerIndexTriggerBitMask = (1 << pointerId) | this.mPointerIndexTriggerBitMask;
                }
            }
        }

        boolean isTouchingInArea() {
            return this.mIsTouchingInArea;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class Injector {
        private View mTouchAreaView;
        private boolean mViewHasShown;
        private WindowManager mWindowManager;

        public Injector() {
            TouchCoverProtectionHelper.this = r3;
            this.mWindowManager = (WindowManager) r3.mContext.getSystemService("window");
            LinearLayout linearLayout = new LinearLayout(r3.mContext);
            this.mTouchAreaView = linearLayout;
            linearLayout.setBackgroundColor(-65536);
            this.mTouchAreaView.setFocusableInTouchMode(true);
            this.mTouchAreaView.setOnTouchListener(new View.OnTouchListener() { // from class: com.android.server.display.TouchCoverProtectionHelper.Injector.1
                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
            this.mTouchAreaView.setSystemUiVisibility(6);
        }

        public void show() {
            if (!this.mViewHasShown) {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.right - TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.left, TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.bottom - TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.top, 2008, 8455424, -3);
                params.gravity = 51;
                params.setTitle("touch-cover-protection-rect");
                DisplayCutout displayCutout = TouchCoverProtectionHelper.this.mLogicalDisplay.getDisplayInfoLocked().displayCutout;
                int insetTop = 0;
                int insetLeft = 0;
                if (displayCutout != null) {
                    insetTop = displayCutout.getSafeInsetTop();
                    insetLeft = displayCutout.getSafeInsetLeft();
                }
                params.x = TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.left - insetLeft;
                params.y = TouchCoverProtectionHelper.this.mTouchCoverProtectionRect.top - insetTop;
                this.mWindowManager.addView(this.mTouchAreaView, params);
                this.mViewHasShown = true;
            }
        }

        public void hide() {
            if (this.mViewHasShown) {
                this.mWindowManager.removeView(this.mTouchAreaView);
                this.mViewHasShown = false;
            }
        }
    }
}
