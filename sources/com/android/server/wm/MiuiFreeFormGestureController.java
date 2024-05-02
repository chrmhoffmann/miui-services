package com.android.server.wm;

import android.app.IMiuiActivityObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.ArraySet;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.IDisplayWindowListener;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import com.android.server.wm.MiuiCvwSnapTargetPool;
import com.android.server.wm.MiuiFreeFormGestureController;
import com.google.android.collect.Sets;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import miui.app.MiuiFreeFormManager;
/* loaded from: classes.dex */
public class MiuiFreeFormGestureController implements MiuiFreeformGestureControllerStub {
    public static boolean DEBUG = false;
    private static final String TAG = "MiuiFreeFormGestureController";
    DisplayContent mDisplayContent;
    private FreeFormReceiver mFreeFormReceiver;
    MiuiFreeFormGesturePointerEventListener mGestureListener;
    Handler mHandler;
    InsetsStateController mInsetsStateController;
    private boolean mLastIsWideScreen;
    MiuiFreeFormFlashBackHelper mMiuiFreeFormFlashBackHelper;
    MiuiFreeFormManagerService mMiuiFreeFormManagerService;
    MiuiFreeFormShadowHelper mMiuiFreeFormShadowHelper;
    MiuiFreeFormSwitchAppHelper mMiuiFreeFormSwitchAppHelper;
    MiuiFreeformPinManagerService mMiuiFreeformPinManagerService;
    MiuiMultiWindowSwitchManager mMiuiMultiWindowSwitchManager;
    WindowManagerService mService;
    MiuiFreeformTrackManager mTrackManager;
    MiuiFreeFormWindowController mWindowController;
    private boolean mCouldBeenFocusWindow = true;
    private boolean mDisableScreenRotation = false;
    private boolean mDisableScreenRotationUpadate = false;
    private Configuration mLastConfiguration = new Configuration();
    private ContentObserver mOneHandyModeActivatedObserver = new AnonymousClass1(new Handler((Handler.Callback) null, false));
    private ContentObserver mSuperPowerOpenObserver = new AnonymousClass2(new Handler((Handler.Callback) null, false));
    private ContentObserver mFocusModeObserver = new AnonymousClass3(new Handler((Handler.Callback) null, false));
    private IDisplayWindowListener mDisplayWindowListener = new IDisplayWindowListener.Stub() { // from class: com.android.server.wm.MiuiFreeFormGestureController.7
        public void onDisplayConfigurationChanged(int displayId, Configuration newConfig) {
            boolean isWideScreen;
            Iterator<MiuiFreeFormActivityStack> it;
            List<MiuiFreeFormActivityStack> stackList;
            boolean isWideScreen2;
            String str;
            Rect bounds;
            int rotation;
            Exception e;
            float widthLimit;
            String str2 = MiuiFreeFormGestureController.TAG;
            Slog.d(str2, "onDisplayConfigurationChanged: displayId: " + displayId + "newConfig: " + newConfig);
            if (displayId != 0 || MiuiFreeFormGestureController.this.mLastIsWideScreen == (isWideScreen = MiuiMultiWindowUtils.isWideScreen(newConfig))) {
                return;
            }
            MiuiFreeFormGestureController.this.mLastIsWideScreen = isWideScreen;
            Slog.d(str2, "onDisplayConfigurationChanged: mLastIsWideScreen= " + MiuiFreeFormGestureController.this.mLastIsWideScreen);
            List<MiuiFreeFormActivityStack> stackList2 = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
            for (Iterator<MiuiFreeFormActivityStack> it2 = stackList2.iterator(); it2.hasNext(); it2 = it) {
                MiuiFreeFormActivityStack mffas = it2.next();
                Rect bounds2 = new Rect();
                if (mffas.mTask == null) {
                    isWideScreen2 = isWideScreen;
                    stackList = stackList2;
                    it = it2;
                } else {
                    bounds2.set(mffas.mTask.getBounds());
                    if (mffas.mMiuiFreeFromWindowMode == 0) {
                        int rotation2 = MiuiFreeFormGestureController.this.mDisplayContent.getRotation();
                        int statusBarHeight = MiuiMultiWindowUtils.getInsetValueFromServer(MiuiFreeFormGestureController.this.mDisplayContent.getDisplayUiContext(), WindowInsets.Type.statusBars());
                        int navBarHeight = MiuiMultiWindowUtils.getNavBarResHeight(MiuiFreeFormGestureController.this.mDisplayContent.getDisplayUiContext());
                        boolean deviceVertical = rotation2 == 0 || rotation2 == 2;
                        String packageName = mffas.getStackPackageName();
                        float widthLimit2 = MiuiFreeFormGestureController.this.mGestureListener.mFreeFormAccessibleArea.width();
                        isWideScreen2 = isWideScreen;
                        stackList = stackList2;
                        float[] visualSize = {bounds2.width() * mffas.mFreeFormScale, bounds2.height() * mffas.mFreeFormScale};
                        boolean deviceVertical2 = deviceVertical;
                        if (MiuiCvwGestureController.isMiuiCvwFeatureEnable()) {
                            MiuiCvwGestureController miuiCvwGestureController = (MiuiCvwGestureController) MiuiFreeFormGestureController.this.mService.mMiuiCvwGestureController;
                            it = it2;
                            MiuiCvwSnapTargetPool.SnapTarget snapTarget = miuiCvwGestureController.mCvwPolicy.getSnapTargetByRatio(bounds2.width() / bounds2.height());
                            if (snapTarget != null) {
                                widthLimit2 = snapTarget.getMaxWidth();
                            }
                            str = str2;
                            widthLimit = widthLimit2;
                        } else {
                            it = it2;
                            RectF sizeBeforeScaling = MiuiMultiWindowUtils.getPossibleBounds(MiuiFreeFormGestureController.this.mService.mContext, deviceVertical2, mffas.mIsLandcapeFreeform, packageName);
                            str = str2;
                            bounds2.set(bounds2.left, bounds2.top, (int) (bounds2.left + sizeBeforeScaling.width()), (int) (bounds2.top + sizeBeforeScaling.height()));
                            int[] maxMinWidth = MiuiFreeFormGestureController.this.mGestureListener.getMaxMinWidthSize(mffas.mIsLandcapeFreeform, deviceVertical2, packageName);
                            widthLimit = maxMinWidth[0];
                        }
                        rotation = 1;
                        float scale = MiuiMultiWindowUtils.adjustFreeFormBounds(visualSize, bounds2, mffas.mFreeFormScale, MiuiFreeFormGestureController.this.mGestureListener.mFreeFormAccessibleArea, MiuiMultiWindowUtils.getFreeFormAccessibleArea(MiuiFreeFormGestureController.this.mService.mContext, rotation2, statusBarHeight, navBarHeight), (int) widthLimit);
                        if (scale != 1.0d) {
                            MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.updateMiuiFreeFormStackScale(mffas.mStackID, mffas.mFreeFormScale * scale);
                        }
                        bounds = bounds2;
                    } else {
                        str = str2;
                        isWideScreen2 = isWideScreen;
                        stackList = stackList2;
                        it = it2;
                        rotation = 1;
                        if (mffas.mMiuiFreeFromWindowMode != 1) {
                            bounds = bounds2;
                        } else {
                            int statusBarHeight2 = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(MiuiFreeFormGestureController.this.mInsetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(MiuiFreeFormGestureController.this.mDisplayContent.mDisplayFrames));
                            bounds = bounds2;
                            MiuiMultiWindowUtils.getFreeformRect(MiuiFreeFormGestureController.this.mService.mContext, false, false, false, mffas.mIsLandcapeFreeform, bounds2, mffas.getStackPackageName(), true, statusBarHeight2);
                            MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.updateMiuiFreeFormStackScale(mffas.mStackID, MiuiMultiWindowUtils.getOriFreeformScale(MiuiFreeFormGestureController.this.mService.mContext, mffas.mIsLandcapeFreeform));
                        }
                    }
                    if (mffas.mMiuiFreeFromWindowMode == 0 || mffas.mMiuiFreeFromWindowMode == rotation) {
                        try {
                            str2 = str;
                        } catch (Exception e2) {
                            e = e2;
                            str2 = str;
                        }
                        try {
                            Slog.d(str2, "resizeTask for fold displayId " + displayId + " newConfig: " + newConfig + " StackID: " + mffas.mStackID + " bounds: " + bounds);
                            MiuiFreeFormGestureController.this.mGestureListener.mService.mActivityManager.resizeTask(mffas.mStackID, bounds, 2);
                        } catch (Exception e3) {
                            e = e3;
                            e.printStackTrace();
                            isWideScreen = isWideScreen2;
                            stackList2 = stackList;
                        }
                    } else {
                        str2 = str;
                    }
                }
                isWideScreen = isWideScreen2;
                stackList2 = stackList;
            }
        }

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onFixedRotationStarted(int displayId, int newRotation) {
        }

        public void onFixedRotationFinished(int displayId) {
        }

        public void onKeepClearAreasChanged(int displayId, List<Rect> restricted, List<Rect> unrestricted) {
        }
    };
    private final IMiuiActivityObserver mMiuiActivityObserver = new IMiuiActivityObserver.Stub() { // from class: com.android.server.wm.MiuiFreeFormGestureController.9
        public void activityIdle(Intent intent) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        public void activityResumed(Intent intent) throws RemoteException {
            ComponentName cn;
            List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
            for (MiuiFreeFormActivityStack mffas : stackList) {
                if (mffas.mMiuiFreeFromWindowMode == 0) {
                    ComponentName cn2 = intent.getComponent();
                    if (cn2 != null && ((MiuiFreeFormGestureDetector.PARENTALCONTROLS_PACKAGE.equals(cn2.getPackageName()) && MiuiFreeFormGestureDetector.PARENTALCONTROLS_ACTIVITY.equals(cn2.getClassName())) || (MiuiFreeFormGestureDetector.FAMILYSMILE_PACKAGE.equals(cn2.getPackageName()) && MiuiFreeFormGestureDetector.FAMILYSMILE_ACTIVITY.equals(cn2.getClassName())))) {
                        Slog.d(MiuiFreeFormGestureController.TAG, "exit from familysmile and parentalcontrols activity resumed mCurrentWindowMode == GESTURE_WINDOWING_MODE_FREEFORM");
                        MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas);
                    }
                } else if (mffas.mMiuiFreeFromWindowMode == 1 && (cn = intent.getComponent()) != null && ((MiuiFreeFormGestureDetector.PARENTALCONTROLS_PACKAGE.equals(cn.getPackageName()) && MiuiFreeFormGestureDetector.PARENTALCONTROLS_ACTIVITY.equals(cn.getClassName())) || (MiuiFreeFormGestureDetector.FAMILYSMILE_PACKAGE.equals(cn.getPackageName()) && MiuiFreeFormGestureDetector.FAMILYSMILE_ACTIVITY.equals(cn.getClassName())))) {
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit from familysmile and parentalcontrols activity resumed mCurrentWindowMode == GESTURE_WINDOWING_MODE_SMALL_FREEFORM");
                    MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas);
                }
            }
        }

        public void activityPaused(Intent intent) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        public void activityStopped(Intent intent) throws RemoteException {
            throw new UnsupportedOperationException();
        }

        public void activityDestroyed(Intent intent) throws RemoteException {
            throw new UnsupportedOperationException();
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreeFormGestureController> {

        /* compiled from: MiuiFreeFormGestureController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreeFormGestureController INSTANCE = new MiuiFreeFormGestureController();
        }

        public MiuiFreeFormGestureController provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiFreeFormGestureController provideNewInstance() {
            return new MiuiFreeFormGestureController();
        }
    }

    public void init(WindowManagerService service) {
        this.mService = service;
        DisplayContent defaultDisplayContentLocked = service.getDefaultDisplayContentLocked();
        this.mDisplayContent = defaultDisplayContentLocked;
        this.mInsetsStateController = defaultDisplayContentLocked.getInsetsStateController();
        HandlerThread handlerThread = new HandlerThread(TAG, -4);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        this.mFreeFormReceiver = new FreeFormReceiver();
        Context context = this.mService.mContext;
        FreeFormReceiver freeFormReceiver = this.mFreeFormReceiver;
        context.registerReceiver(freeFormReceiver, freeFormReceiver.mFilter, null, this.mHandler);
        MiuiFreeFormManagerService miuiFreeFormManagerService = (MiuiFreeFormManagerService) this.mService.mAtmService.mMiuiFreeFormManagerService;
        this.mMiuiFreeFormManagerService = miuiFreeFormManagerService;
        miuiFreeFormManagerService.init(this.mHandler);
        this.mMiuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mService.mAtmService);
        this.mMiuiFreeFormSwitchAppHelper = new MiuiFreeFormSwitchAppHelper(this);
        this.mMiuiFreeFormFlashBackHelper = new MiuiFreeFormFlashBackHelper(this);
        this.mMiuiFreeformPinManagerService = new MiuiFreeformPinManagerService(this, handlerThread.getLooper());
        this.mMiuiFreeFormShadowHelper = new MiuiFreeFormShadowHelper(this);
        registerFoldDisplayReceiver();
        registerEventListener(service);
        registerActivityObserver();
        registerFreeformCloudDataObserver();
        this.mWindowController = new MiuiFreeFormWindowController(service.mContext, this);
        service.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("one_handed_mode_activated"), false, this.mOneHandyModeActivatedObserver, 0);
        service.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("power_supersave_mode_open"), false, this.mSuperPowerOpenObserver, 0);
        service.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(MiuiFreeFormGestureDetector.FOCUS_MODE_STATUS), false, this.mFocusModeObserver, 0);
    }

    /* renamed from: com.android.server.wm.MiuiFreeFormGestureController$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass1(Handler handler) {
            super(handler);
            MiuiFreeFormGestureController.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            ContentResolver cr = MiuiFreeFormGestureController.this.mService.mContext.getContentResolver();
            boolean z = false;
            if (Settings.Secure.getIntForUser(cr, "one_handed_mode_activated", 0, cr.getUserId()) == 1) {
                z = true;
            }
            boolean isOneHandyMode = z;
            if (isOneHandyMode) {
                MiuiFreeFormGestureController.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$1$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiFreeFormGestureController.AnonymousClass1.this.m1652x5cfc7b6f();
                    }
                });
            }
        }

        /* renamed from: lambda$onChange$0$com-android-server-wm-MiuiFreeFormGestureController$1 */
        public /* synthetic */ void m1652x5cfc7b6f() {
            List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
            for (MiuiFreeFormActivityStack mffas : stackList) {
                Slog.d(MiuiFreeFormGestureController.TAG, "handle freeform or minifreeform to pin caused by oneHandy mode activated");
                MiuiFreeFormGestureController.this.mMiuiFreeformPinManagerService.handleFreeFormToPin(mffas);
            }
        }
    }

    /* renamed from: com.android.server.wm.MiuiFreeFormGestureController$2 */
    /* loaded from: classes.dex */
    public class AnonymousClass2 extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass2(Handler handler) {
            super(handler);
            MiuiFreeFormGestureController.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            boolean z = false;
            if (Settings.System.getIntForUser(MiuiFreeFormGestureController.this.mService.mContext.getContentResolver(), "power_supersave_mode_open", 0, 0) != 0) {
                z = true;
            }
            boolean mIsSuperPower = z;
            if (mIsSuperPower) {
                MiuiFreeFormGestureController.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$2$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiFreeFormGestureController.AnonymousClass2.this.m1653x5cfc7b70();
                    }
                });
            }
        }

        /* renamed from: lambda$onChange$0$com-android-server-wm-MiuiFreeFormGestureController$2 */
        public /* synthetic */ void m1653x5cfc7b70() {
            List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
            for (MiuiFreeFormActivityStack mffas : stackList) {
                if (mffas.isInFreeFormMode()) {
                    MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas);
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit freeform from power_supersave_mode_open");
                } else if (mffas.isInMiniFreeFormMode()) {
                    MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas);
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit smallfreeform from power_supersave_mode_open");
                }
            }
        }
    }

    /* renamed from: com.android.server.wm.MiuiFreeFormGestureController$3 */
    /* loaded from: classes.dex */
    public class AnonymousClass3 extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        AnonymousClass3(Handler handler) {
            super(handler);
            MiuiFreeFormGestureController.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            boolean z = false;
            if (Settings.Global.getInt(MiuiFreeFormGestureController.this.mService.mContext.getContentResolver(), MiuiFreeFormGestureDetector.FOCUS_MODE_STATUS, 0) == 1) {
                z = true;
            }
            boolean mIsFocusMode = z;
            if (mIsFocusMode) {
                MiuiFreeFormGestureController.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$3$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiFreeFormGestureController.AnonymousClass3.this.m1654x5cfc7b71();
                    }
                });
            }
        }

        /* renamed from: lambda$onChange$0$com-android-server-wm-MiuiFreeFormGestureController$3 */
        public /* synthetic */ void m1654x5cfc7b71() {
            List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
            for (MiuiFreeFormActivityStack mffas : stackList) {
                if (mffas.isInFreeFormMode()) {
                    MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas);
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit freeform from focusmode");
                } else if (mffas.isInMiniFreeFormMode()) {
                    MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas);
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit smallfreeform from focusmode");
                }
            }
        }
    }

    public void onATMSSystemReady() {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController.4
            @Override // java.lang.Runnable
            public void run() {
                MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
                miuiFreeFormGestureController.mTrackManager = new MiuiFreeformTrackManager(miuiFreeFormGestureController.mService.mContext, MiuiFreeFormGestureController.this);
            }
        }, 5000L);
    }

    private void registerEventListener(WindowManagerService service) {
        try {
            MiuiFreeFormGesturePointerEventListener miuiFreeFormGesturePointerEventListener = new MiuiFreeFormGesturePointerEventListener(service, this);
            this.mGestureListener = miuiFreeFormGesturePointerEventListener;
            this.mDisplayContent.registerPointerEventListener(miuiFreeFormGesturePointerEventListener);
            Slog.d(TAG, "registerEventListener");
        } catch (Exception e) {
            e.printStackTrace();
            this.mDisplayContent.registerPointerEventListener(this.mGestureListener);
            Slog.d(TAG, "registerEventListener again");
        }
    }

    public void setOriginalBounds(Rect rect, MiuiFreeFormActivityStackStub mffasStub) {
        if (mffasStub instanceof MiuiFreeFormActivityStack) {
            MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) mffasStub;
            this.mGestureListener.mFreeFormWindowMotionHelper.setOriginalBounds(rect, stack);
        }
    }

    public void hideStack(MiuiFreeFormActivityStackStub mffasStub) {
        if (mffasStub instanceof MiuiFreeFormActivityStack) {
            MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) mffasStub;
            this.mGestureListener.mGestureAnimator.hideStack(stack);
        }
    }

    public boolean[] stackOnConfigurationChanged(Task task, int prevWindowingMode, int overrideWindowingMode) {
        boolean isEnterFreeformMode = false;
        boolean isOutFreeformMode = false;
        boolean[] isFreeformModeChanged = new boolean[2];
        if ((prevWindowingMode == 5) != (overrideWindowingMode == 5)) {
            if (overrideWindowingMode == 5) {
                isEnterFreeformMode = true;
                task.setAlwaysOnTop(true);
            } else {
                isOutFreeformMode = true;
            }
        }
        isFreeformModeChanged[0] = isEnterFreeformMode;
        isFreeformModeChanged[1] = isOutFreeformMode;
        return isFreeformModeChanged;
    }

    public void closeLastFreeformTask() {
        Slog.d(TAG, "closeLastFreeformTask");
        this.mMiuiFreeFormFlashBackHelper.stopFlashBackService(this.mService.mContext, this.mHandler);
        this.mWindowController.removeHotSpotView();
        this.mWindowController.removeBottomBarHotSpotView();
        this.mWindowController.removeOpenCloseTipWindow();
        this.mWindowController.removeOverlayViewChecked();
    }

    public void startFirstFreeformTask() {
        Slog.d(TAG, "startFirstFreeformTask");
        this.mWindowController.addHotSpotView();
        this.mWindowController.addBottomBarHotSpotView();
        this.mWindowController.addOverlayView();
    }

    /* JADX WARN: Removed duplicated region for block: B:57:0x019c  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x01a7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean getFreeformStackBounds(com.android.server.wm.Task r27, boolean r28, int r29, int r30, android.graphics.Rect r31) {
        /*
            Method dump skipped, instructions count: 460
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormGestureController.getFreeformStackBounds(com.android.server.wm.Task, boolean, int, int, android.graphics.Rect):boolean");
    }

    private float adjustFreeFormBoundsAfterRotation(DisplayContent dc, float[] visualSize, int oldRotation, int newRotation, Rect currentBounds, float currentScale, Rect accessibleAreaBefore, int maxWidthLimit) {
        if ((oldRotation - newRotation) % 2 == 0) {
            return 1.0f;
        }
        return MiuiMultiWindowUtils.adjustFreeFormBounds(visualSize, currentBounds, currentScale, accessibleAreaBefore, MiuiFreeformServiceStub.getInstance().getFreeFormAccessibleArea(dc.getDisplayUiContext(), dc), maxWidthLimit);
    }

    public void adjustFreeformStackOrientation(final MiuiFreeFormActivityStack ffas) {
        traverseTask(ffas.mTask, new Consumer() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda3
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiFreeFormGestureController.lambda$adjustFreeformStackOrientation$0(MiuiFreeFormActivityStack.this, (ActivityRecord) obj);
            }
        });
    }

    public static /* synthetic */ void lambda$adjustFreeformStackOrientation$0(MiuiFreeFormActivityStack ffas, ActivityRecord activityRecord) {
        int orientation = activityRecord.getOrientation();
        Slog.d(TAG, "adjustFreeformStackOrientation: activityRecord= " + activityRecord + " orientation= " + orientation + " ffas.mTopActivityOrientation= " + ffas.mTopActivityOrientation + " ffas.mIsLandcapeFreeform= " + ffas.mIsLandcapeFreeform);
        if (MiuiMultiWindowAdapter.LIST_ABOUT_IGNORE_REQUEST_ORIENTATION_IN_FREEFORM.contains(ffas.getStackPackageName()) && MiuiMultiWindowUtils.isOrientationLandscape(orientation)) {
            Slog.d(TAG, "adjustFreeformStackOrientation: ffas in LIST_ABOUT_IGNORE_REQUEST_ORIENTATION_IN_FREEFORM");
        } else if (orientation != ffas.mTopActivityOrientation) {
            ffas.mTopActivityOrientation = orientation;
            ffas.mIsLandcapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(orientation);
        }
    }

    public boolean setFreeformStackBoundsForAddWindow(WindowState win, Task task, boolean alreadySetFreeformBounds) {
        if (task == null || task.getChildCount() != 1 || alreadySetFreeformBounds) {
            return false;
        }
        ActivityRecord topActivity = task.getTopFullscreenActivity();
        if (topActivity == null || topActivity.findMainWindow() != win) {
            return false;
        }
        Slog.d(TAG, "setFreeformStackBoundsForAddWindow setTaskBounds() topActivity: " + topActivity);
        boolean isFreeformLandscape = MiuiMultiWindowUtils.isOrientationLandscape(topActivity.getRequestedOrientation());
        MiuiFreeFormActivityStack ffas = (MiuiFreeFormActivityStack) this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(task.getRootTaskId());
        int statusBarHeight = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(this.mInsetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(this.mDisplayContent.mDisplayFrames));
        Rect bounds = MiuiMultiWindowUtils.getFreeformRect(this.mService.mContext, false, false, false, isFreeformLandscape, (Rect) null, ffas.getStackPackageName(), true, statusBarHeight);
        ffas.mIsLandcapeFreeform = isFreeformLandscape;
        task.setBounds(bounds);
        Slog.d(TAG, "setFreeformStackBoundsForAddWindow setTaskBounds() bounds: " + bounds + " isFreeformLandscape: " + isFreeformLandscape);
        return true;
    }

    private void registerFoldDisplayReceiver() {
        if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
            Slog.d(TAG, "registerFoldDisplayReceiver for fold device");
            this.mService.registerDisplayWindowListener(this.mDisplayWindowListener);
        }
    }

    public boolean isInFreeFormControlRegon(float x, float y) {
        return this.mGestureListener.mFreeFormWindowMotionHelper.isInFreeFormControlRegon(x, y);
    }

    public void setDisableScreenRotation(boolean disableScreenRotation) {
        this.mDisableScreenRotation = disableScreenRotation;
        setDisableScreenRotationUpdate(disableScreenRotation);
    }

    public boolean isScreenRotationDisabled() {
        return this.mDisableScreenRotation;
    }

    public void setDisableScreenRotationUpdate(boolean disableScreenRotationUpdate) {
        this.mDisableScreenRotationUpadate = disableScreenRotationUpdate;
    }

    public boolean isScreenRotationDisabledUpadate() {
        return this.mDisableScreenRotationUpadate;
    }

    public void startShowFullScreenWindow(int action, MiuiFreeFormActivityStack stack) {
        this.mGestureListener.startShowFullScreenWindow(action, stack);
    }

    public boolean isFreeformCouldBeenFocusWindow() {
        return this.mCouldBeenFocusWindow;
    }

    public boolean isSwitchingApp() {
        return this.mMiuiFreeFormSwitchAppHelper.isSwitchingApp();
    }

    public boolean isSwitchingApp(Task task) {
        return this.mMiuiFreeFormSwitchAppHelper.isSwitchingApp(task);
    }

    public void launchSmallFreeFormWindow(MiuiFreeFormActivityStackStub mffasStub, boolean launchFromControlCenter) {
        Slog.d(TAG, "launchSmallFreeFormWindow");
        if (mffasStub instanceof MiuiFreeFormActivityStack) {
            MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) mffasStub;
            this.mGestureListener.launchSmallFreeFormWindow(mffas, launchFromControlCenter);
        }
    }

    public void launchSmallFreeformByCVW(MiuiFreeFormActivityStack mffas, MiuiCvwAnimator animator, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler, Rect taskBounds, Rect miniFreeformBounds, float freeFormScale, boolean islaunchSmallFreeformByFreeform) {
        this.mGestureListener.mSmallFreeFormWindowMotionHelper.launchSmallFreeformByCVW(mffas, animator, miuiCvwGestureHandler, taskBounds, miniFreeformBounds, freeFormScale, islaunchSmallFreeformByFreeform);
    }

    public int getMiuiFreeFormPositionX(int orientation) {
        return this.mMiuiFreeFormFlashBackHelper.getMiuiFreeFormPositionX(orientation);
    }

    public int getMiuiFreeFormPositionY(int orientation) {
        return this.mMiuiFreeFormFlashBackHelper.getMiuiFreeFormPositionY(orientation);
    }

    public void launchFlashBackFromBackGroundAnim(int orientation) {
        this.mMiuiFreeFormFlashBackHelper.launchFlashBackFromBackGroundAnim(orientation, this.mHandler);
    }

    public void setFlashBackMode(boolean flashBackMode) {
        MiuiFreeFormFlashBackHelper.sIsFlashBackMode = flashBackMode;
    }

    public boolean getFlashBackMode() {
        return MiuiFreeFormFlashBackHelper.sIsFlashBackMode;
    }

    public void setRequestedOrientation(final int requestedOrientation, final Task task, final boolean noAnimation) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController.5
            @Override // java.lang.Runnable
            public void run() {
                MiuiFreeFormGestureController.this.mGestureListener.setRequestedOrientation(requestedOrientation, task, noAnimation);
            }
        });
    }

    public void showScreenSurface(MiuiFreeFormActivityStackStub mffasStub) {
        if (mffasStub instanceof MiuiFreeFormActivityStack) {
            MiuiFreeFormActivityStack mffas = (MiuiFreeFormActivityStack) mffasStub;
            this.mGestureListener.showScreenSurface(mffas);
        }
    }

    public void applyTransaction() {
        this.mGestureListener.mGestureAnimator.applyTransaction();
    }

    public void hideScreenSurface(MiuiFreeFormActivityStack mffas) {
        this.mGestureListener.hideScreenSurface(mffas);
    }

    public void notifyExitFreeFormApplicationStart(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(3));
        mffas.setStackFreeFormMode(-1);
        exitFreeForm(mffas);
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(3, mffas);
    }

    public void notifyExitSmallFreeFormApplicationStart(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(5));
        mffas.setStackFreeFormMode(-1);
        exitFreeForm(mffas);
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(5, mffas);
    }

    public void notifyStartFreeformToSmallFreeFormWindow(int action, MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(action));
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(action, mffas);
    }

    public void notifyStartSmallFreeFormToFreeformWindow(int action, MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(action));
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(action, mffas);
    }

    public void notifyFreeFormApplicationResizeStart(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(6));
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(6, mffas);
    }

    public void notifyFreeFormApplicationResizeEnd(long resizeTime, MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, MiuiFreeFormManager.actionToString(7));
        this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(7, mffas);
    }

    public void notifyFullScreenWidnowModeStart(int action, MiuiFreeFormActivityStack mffas) {
        notifyFullScreenWidnowModeStart(action, mffas, false);
    }

    public void notifyFullScreenWidnowModeStart(int action, MiuiFreeFormActivityStack mffas, boolean enterSplit) {
        Task task;
        ActivityRecord activityRecord;
        Slog.d(TAG, MiuiFreeFormManager.actionToString(action));
        try {
            synchronized (this.mService.mGlobalLock) {
                mffas.setStackFreeFormMode(-1);
                if (!enterSplit) {
                    this.mService.mAtmService.setTaskWindowingMode(mffas.mStackID, 0, true);
                    this.mService.mAtmService.mTaskSupervisor.scheduleIdle();
                }
                Task task2 = this.mService.mAtmService.mTaskSupervisor.mRootWindowContainer.anyTaskForId(mffas.mStackID, 0);
                this.mService.mAtmService.resumeAppSwitches();
                if (task2 != null) {
                    if (enterSplit) {
                        task2.setAlwaysOnTop(false);
                    }
                    task2.setBounds((Rect) null);
                    Configuration c = new Configuration(task2.getRequestedOverrideConfiguration());
                    c.windowConfiguration.setWindowingMode(0);
                    task2.onRequestedOverrideConfigurationChanged(c);
                    task2.sendTaskAppeared();
                }
                this.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(action, mffas);
                this.mService.mAtmService.getTaskChangeNotificationController().notifyTaskStackChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (MiuiMultiWindowUtils.isGlobalLauncher() && (task = this.mService.mAtmService.mTaskSupervisor.mRootWindowContainer.anyTaskForId(mffas.mStackID, 0)) != null && (activityRecord = task.getTopMostActivity()) != null) {
            Slog.d(TAG, "notifyFullScreenWidnowModeStart: activityResumed: " + activityRecord);
            ActivityTaskManagerServiceStub.get().getMiuiActivityController().activityResumed(activityRecord);
            ActivityTaskManagerServiceStub.get().onForegroundActivityChangedLocked(activityRecord);
        }
    }

    private void exitFreeForm(MiuiFreeFormActivityStack mffas) {
        exitFreeForm(mffas, "moveFreeformToFullScreen");
    }

    public void exitFreeForm(MiuiFreeFormActivityStack mffas, String reasion) {
        Slog.d(TAG, "exitFreeForm");
        synchronized (this.mService.mGlobalLock) {
            if (mffas != null) {
                if (mffas.mTask != null) {
                    if (mffas.mTask.getBaseIntent() != null) {
                        mffas.mTask.getBaseIntent().setMiuiFlags(mffas.mTask.getBaseIntent().getMiuiFlags() & (-257));
                    }
                    Slog.d(TAG, "exitFreeForm freeform stack :" + mffas.mTask + " for " + reasion);
                    moveFreeformToFullScreen(mffas);
                    if (mffas.mHadHideStack) {
                        mffas.mHadHideStack = false;
                        mffas.mStartExitStack = false;
                    }
                }
            }
        }
    }

    public void moveTaskToBack(MiuiFreeFormActivityStack mffas) {
        if (DEBUG) {
            Slog.d(TAG, "moveActivityTaskToBack mffas=" + mffas);
        }
        synchronized (this.mService.mGlobalLock) {
            if (mffas != null) {
                if (mffas.mTask != null) {
                    mffas.mTask.setAlwaysOnTop(false);
                    if (mffas.mTask.getDisplayArea() != null) {
                        mffas.mTask.moveTaskToBack(mffas.mTask);
                    }
                }
            }
        }
    }

    public void moveTaskToFront(MiuiFreeFormActivityStack mffas) {
        if (DEBUG) {
            Slog.d(TAG, "moveTaskToFront mffas=" + mffas);
        }
        synchronized (this.mService.mGlobalLock) {
            if (mffas != null) {
                if (mffas.mTask != null) {
                    mffas.mTask.setAlwaysOnTop(true);
                    Task rootTask = mffas.mTask.getRootTask();
                    rootTask.moveToFront("moveTaskToFront mffas: " + mffas, mffas.mTask);
                    rootTask.mDisplayContent.ensureActivitiesVisible((ActivityRecord) null, 0, true, true);
                }
            }
        }
    }

    public void notifyAppStopped(MiuiFreeFormActivityStack mffas) {
        if (DEBUG) {
            Slog.d(TAG, "notifyAppStopped mffas=" + mffas);
        }
        if (mffas != null && mffas.mHadHideStack) {
            mffas.mHadHideStack = false;
            mffas.mStartExitStack = false;
            this.mGestureListener.mGestureAnimator.showStack(mffas);
            this.mGestureListener.mGestureAnimator.applyTransaction();
        }
    }

    public void moveToFront(MiuiFreeFormActivityStack mffas) {
        if (DEBUG) {
            Slog.d(TAG, "moveToFront mffas=" + mffas);
        }
        synchronized (this.mService.mGlobalLock) {
            if (mffas.mTask != null && !mffas.mTask.isAlwaysOnTop()) {
                mffas.mTask.setAlwaysOnTop(true);
            }
        }
    }

    public boolean startActivityUncheckedBefore(MiuiFreeFormActivityStack mffas) {
        return mffas.mStartExitStack;
    }

    static boolean setMiuiConfigFlag(Task object, int miuiConfigFlag, boolean isSetToStack) {
        try {
            Method method = Task.class.getDeclaredMethod("setMiuiConfigFlag", Integer.TYPE, Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(object, Integer.valueOf(miuiConfigFlag), Boolean.valueOf(isSetToStack));
            return true;
        } catch (Exception e) {
            Slog.d(TAG, "setMiuiConfigFlag:" + e.toString());
            return false;
        }
    }

    private void moveFreeformToFullScreen(MiuiFreeFormActivityStack mffas) {
        Task rootTask = mffas.mTask;
        if (rootTask == null || rootTask.getDisplayArea() == null) {
            return;
        }
        rootTask.setAlwaysOnTop(false);
        rootTask.setForceHidden(1, true);
        rootTask.ensureActivitiesVisible((ActivityRecord) null, 0, true);
        this.mService.mAtmService.mTaskSupervisor.activityIdleInternal((ActivityRecord) null, false, true, (Configuration) null);
        this.mService.mAtmService.deferWindowLayout();
        try {
            ArraySet<Task> tasks = Sets.newArraySet(new Task[]{rootTask});
            this.mService.mTaskSnapshotController.snapshotTasks(tasks);
            this.mService.mTaskSnapshotController.addSkipClosingAppSnapshotTasks(tasks);
            if (rootTask != null) {
                rootTask.forAllActivities(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda4
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        MiuiFreeFormGestureController.this.m1649x3fcd09eb((ActivityRecord) obj);
                    }
                });
            }
            this.mGestureListener.mGestureAnimator.applyTransaction();
            Configuration c = new Configuration(rootTask.getRequestedOverrideConfiguration());
            c.windowConfiguration.setWindowingMode(0);
            c.windowConfiguration.setBounds((Rect) null);
            rootTask.onRequestedOverrideConfigurationChanged(c);
            rootTask.sendTaskAppeared();
            TaskDisplayArea taskDisplayArea = rootTask.getDisplayArea();
            if (this.mService.mAtmService.isInSplitScreenWindowingMode() && (!rootTask.isResizeable() || ActivityTaskManagerServiceStub.get().inResizeBlackList(mffas.getStackPackageName()))) {
                rootTask.moveTaskToBack(mffas.mTask);
            } else {
                taskDisplayArea.positionTaskBehindHome(rootTask);
            }
            rootTask.setForceHidden(1, false);
            this.mService.mAtmService.mTaskSupervisor.mRootWindowContainer.ensureActivitiesVisible((ActivityRecord) null, 0, true);
            this.mService.mAtmService.mTaskSupervisor.mRootWindowContainer.resumeFocusedTasksTopActivities();
        } catch (Exception e) {
        } catch (Throwable th) {
            this.mService.mAtmService.continueWindowLayout();
            throw th;
        }
        this.mService.mAtmService.continueWindowLayout();
    }

    /* renamed from: lambda$moveFreeformToFullScreen$1$com-android-server-wm-MiuiFreeFormGestureController */
    public /* synthetic */ void m1649x3fcd09eb(ActivityRecord r) {
        this.mGestureListener.mGestureAnimator.hide(r);
    }

    public void displayConfigurationChange(final DisplayContent displayContent, final Configuration configuration) {
        this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController.6
            @Override // java.lang.Runnable
            public void run() {
                Slog.d(MiuiFreeFormGestureController.TAG, "displayConfigurationChange");
                if (displayContent.getDisplayId() != 0) {
                    return;
                }
                int changes = MiuiFreeFormGestureController.this.mLastConfiguration.updateFrom(configuration);
                boolean densitySizeChange = (changes & 4096) != 0;
                if (densitySizeChange) {
                    Slog.d(MiuiFreeFormGestureController.TAG, "densitySizeChange");
                    AppTransitionInjector.initDisplayRoundCorner(MiuiFreeFormGestureController.this.mService.mContext);
                    List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
                    for (MiuiFreeFormActivityStack mffas : stackList) {
                        if (mffas.isInFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas);
                        } else if (mffas.isInMiniFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas);
                        }
                    }
                }
                MiuiFreeFormGestureController.this.mGestureListener.updateScreenParams(displayContent, configuration);
            }
        });
    }

    public void startRemoveOverLayViewIfNeeded() {
        this.mWindowController.startRemoveOverLayViewIfNeeded();
        this.mMiuiFreeFormSwitchAppHelper.startSwitchAppFourStep();
    }

    /* loaded from: classes.dex */
    private final class FreeFormReceiver extends BroadcastReceiver {
        private static final String ACTION_DOWNWARD_MOVEMENT_SMALLWINDOW = "miui.intent.action.down";
        private static final String ACTION_FULLSCREEN_STATE_CHANGE = "com.miui.fullscreen_state_change";
        private static final String ACTION_INPUT_METHOD_VISIBLE_HEIGHT_CHANGED = "miui.intent.action.INPUT_METHOD_VISIBLE_HEIGHT_CHANGED";
        private static final String ACTION_KEYCODE_BACK = "miui.intent.KEYCODE_BACK";
        private static final String ACTION_LAUNCH_FULLSCREEN_FROM_FREEFORM = "miui.intent.action_launch_fullscreen_from_freeform";
        private static final String ACTION_PC_MODE_ENTER = "miui.intent.action.PC_MODE_ENTER";
        private static final String ACTION_UNPIN_FREEFORM = "miui.intent.action_launch_unpin_to_freeform";
        private static final String ACTION_UPWARD_MOVEMENT_SMALLWINDOW = "miui.intent.action.up";
        private static final String APP_STACKID = "rootStackID";
        private static final String APP_USERID = "userID";
        private static final String EXTRA_FULLSCREEN_STATE_NAME = "state";
        private static final String EXTRA_INPUT_METHOD_VISIBLE_HEIGHT = "miui.intent.extra.input_method_visible_height";
        private static final String FREEFORM_STATE_TO_SMALLFREEFORM = "toSmallFreeform";
        private static final String FREEFORM_STATE_TO_TASK_SNAPSHOT = "taskSnapshot";
        private static final String FULLSCREEN_STATE_CROSS_SAFE_AREA = "crossSafeArea";
        private static final String FULLSCREEN_STATE_TO_HOME = "toHome";
        private static final String FULLSCREEN_STATE_TO_RECENTS = "toRecents";
        private static final String PACKAGE_NAME = "packageName";
        IntentFilter mFilter;

        public FreeFormReceiver() {
            MiuiFreeFormGestureController.this = r2;
            IntentFilter intentFilter = new IntentFilter();
            this.mFilter = intentFilter;
            intentFilter.addAction(ACTION_FULLSCREEN_STATE_CHANGE);
            this.mFilter.addAction(ACTION_INPUT_METHOD_VISIBLE_HEIGHT_CHANGED);
            this.mFilter.addAction(ACTION_DOWNWARD_MOVEMENT_SMALLWINDOW);
            this.mFilter.addAction(ACTION_UPWARD_MOVEMENT_SMALLWINDOW);
            this.mFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            this.mFilter.addAction(ACTION_KEYCODE_BACK);
            this.mFilter.addAction(ACTION_LAUNCH_FULLSCREEN_FROM_FREEFORM);
            this.mFilter.addAction(ACTION_UNPIN_FREEFORM);
            this.mFilter.addAction("android.intent.action.USER_PRESENT");
            this.mFilter.addAction("android.intent.action.USER_SWITCHED");
            this.mFilter.addAction(ACTION_PC_MODE_ENTER);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiFreeFormActivityStack mffas;
            Bundle bundle;
            try {
                String action = intent.getAction();
                if ("android.intent.action.USER_PRESENT".equals(action)) {
                    if (MiuiFreeFormGestureController.this.mTrackManager != null) {
                        MiuiFreeFormGestureController.this.mTrackManager.bindOneTrackService();
                    } else {
                        MiuiFreeFormGestureController miuiFreeFormGestureController = MiuiFreeFormGestureController.this;
                        miuiFreeFormGestureController.mTrackManager = new MiuiFreeformTrackManager(miuiFreeFormGestureController.mService.mContext, MiuiFreeFormGestureController.this);
                    }
                }
                MiuiFreeFormGestureController.this.mWindowController.removeOpenCloseTipWindow();
                Slog.d(MiuiFreeFormGestureController.TAG, "handleFreeFormReceiver intent=" + intent);
                if (MiuiMultiWindowUtils.isUserAMonkey()) {
                    return;
                }
                List<MiuiFreeFormActivityStack> stackList = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
                if (stackList.isEmpty()) {
                    return;
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit freeform for user switch new userId: " + intent.getIntExtra("android.intent.extra.user_handle", 0));
                    for (MiuiFreeFormActivityStack mffas2 : stackList) {
                        if (mffas2.isInFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas2);
                        } else if (mffas2.isInMiniFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas2);
                        }
                    }
                } else if (ACTION_PC_MODE_ENTER.equals(action)) {
                    Slog.d(MiuiFreeFormGestureController.TAG, "exit freeform for enter pc mode");
                    for (MiuiFreeFormActivityStack mffas3 : stackList) {
                        if (mffas3.isInFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitApplication(mffas3);
                        } else if (mffas3.isInMiniFreeFormMode()) {
                            MiuiFreeFormGestureController.this.mGestureListener.startExitSmallFreeformApplication(mffas3);
                        }
                    }
                } else if (ACTION_FULLSCREEN_STATE_CHANGE.equals(action)) {
                    if (MiuiFreeFormGestureController.this.mWindowController.inOverlayOpenAnimation() || (bundle = intent.getExtras()) == null) {
                        return;
                    }
                    String stateName = bundle.getString(EXTRA_FULLSCREEN_STATE_NAME);
                    int stackId = bundle.getInt(APP_STACKID);
                    if (!FULLSCREEN_STATE_TO_HOME.equals(stateName) && !FULLSCREEN_STATE_TO_RECENTS.equals(stateName) && !FREEFORM_STATE_TO_SMALLFREEFORM.equals(stateName)) {
                        if (FREEFORM_STATE_TO_TASK_SNAPSHOT.equals(stateName)) {
                            for (MiuiFreeFormActivityStack mffas4 : stackList) {
                                MiuiFreeFormGestureController.this.mGestureListener.reflectHandleSnapshotTaskByFreeform(mffas4.mTask);
                            }
                        }
                    }
                    for (MiuiFreeFormActivityStack mffas5 : stackList) {
                        if (mffas5.isInFreeFormMode()) {
                            if (FREEFORM_STATE_TO_SMALLFREEFORM.equals(stateName)) {
                                if (mffas5.mStackID == stackId && mffas5.mStackControlInfo.mCurrentAnimation == -1) {
                                    MiuiFreeFormGestureController.this.mGestureListener.mSmallFreeFormWindowMotionHelper.startShowFreeFormWindow(mffas5);
                                    MiuiFreeFormGestureController.this.mGestureListener.turnFreeFormToSmallWindow(mffas5);
                                }
                            } else if (mffas5.mStackControlInfo.mCurrentAnimation == -1) {
                                Slog.d(MiuiFreeFormGestureController.TAG, "handleFreeFormToPin stateName: " + stateName);
                                MiuiFreeFormGestureController.this.mMiuiFreeformPinManagerService.handleFreeFormToPin(mffas5);
                            }
                        }
                    }
                } else if (ACTION_INPUT_METHOD_VISIBLE_HEIGHT_CHANGED.equals(action)) {
                    Bundle bundle2 = intent.getExtras();
                    if (bundle2 == null) {
                        return;
                    }
                    for (MiuiFreeFormActivityStack mffas6 : stackList) {
                        if (mffas6.isInMiniFreeFormMode()) {
                            int inputMethodWindowVisibleHeight = bundle2.getInt(EXTRA_INPUT_METHOD_VISIBLE_HEIGHT);
                            MiuiFreeFormGestureController.this.mGestureListener.inputMethodVisibleChanged(inputMethodWindowVisibleHeight, mffas6);
                        }
                    }
                } else if (ACTION_UNPIN_FREEFORM.equals(action)) {
                    Bundle bundle3 = intent.getExtras();
                    if (bundle3 == null) {
                        return;
                    }
                    String packageName = bundle3.getString("packageName");
                    int userId = bundle3.getInt(APP_USERID);
                    int stackId2 = bundle3.getInt(APP_STACKID);
                    if (stackId2 != 0) {
                        mffas = MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStackForMiuiFB(stackId2);
                    } else {
                        mffas = (MiuiFreeFormActivityStack) MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(packageName, userId);
                    }
                    Slog.d(MiuiFreeFormGestureController.TAG, "mffas: " + mffas + " stackId= " + stackId2);
                    if (mffas != null && mffas.inPinMode() && !mffas.mIsRunningPinAnim) {
                        MiuiFreeFormGestureController.this.mMiuiFreeFormManagerService.unPinFloatingWindow(mffas.mTask.mTaskId);
                        return;
                    }
                    Slog.d(MiuiFreeFormGestureController.TAG, "skip form pin to freeform mffas: " + mffas);
                } else if (ACTION_LAUNCH_FULLSCREEN_FROM_FREEFORM.equals(action)) {
                    MiuiFreeFormGestureController.this.startFullscreenFromFreeform(true, null, intent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startFullscreenFromFreeform(boolean isFromBroadCast, MiuiFreeFormActivityStackStub freeFormActivityStackStub, Intent intent) {
        final MiuiFreeFormActivityStack mffas;
        try {
            if (isFromBroadCast && intent != null) {
                Bundle bundle = intent.getExtras();
                if (bundle == null) {
                    return;
                }
                String packageName = bundle.getString("packageName");
                int userId = bundle.getInt("userID");
                int stackId = bundle.getInt("rootStackID");
                if (stackId != 0) {
                    mffas = this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStackForMiuiFB(stackId);
                } else {
                    mffas = (MiuiFreeFormActivityStack) this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(packageName, userId);
                }
            } else {
                mffas = (MiuiFreeFormActivityStack) freeFormActivityStackStub;
            }
            if (mffas == null) {
                return;
            }
            Slog.d(TAG, "startFullscreenFromFreeform: isFromBroadCast= " + isFromBroadCast + " stackId= " + mffas.mTask.getRootTaskId());
            if (mffas.inPinMode()) {
                if (mffas.mIsRunningPinAnim) {
                    Slog.d(TAG, "skip form pin to fullscreen mIsRunningPinAnim: " + mffas);
                    return;
                }
                if (mffas.getPinMode() == 1) {
                    this.mGestureListener.mGestureAnimator.showStack(mffas);
                } else if (mffas.getPinMode() == 2) {
                    this.mGestureListener.mGestureAnimator.showStack(mffas);
                    moveTaskToFront(mffas);
                }
                this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiFreeFormGestureController.this.m1650x2c814732(mffas);
                    }
                });
                return;
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiFreeFormGestureController.this.m1651xb9bbf8b3(mffas);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* renamed from: lambda$startFullscreenFromFreeform$2$com-android-server-wm-MiuiFreeFormGestureController */
    public /* synthetic */ void m1650x2c814732(MiuiFreeFormActivityStack mffas) {
        long timeoutAtTimeMs = System.currentTimeMillis() + 2000;
        synchronized (this.mMiuiFreeformPinManagerService.mLock) {
            mffas.topWindowHasDrawn = false;
            while (!mffas.topWindowHasDrawn) {
                try {
                    long waitMillis = timeoutAtTimeMs - System.currentTimeMillis();
                    if (waitMillis <= 0) {
                        break;
                    }
                    this.mMiuiFreeformPinManagerService.mLock.wait(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        boolean isInFreeFormMode = mffas.isInFreeFormMode();
        float f = MiuiFreeformPinManagerService.EDGE_AREA;
        if (isInFreeFormMode) {
            this.mGestureListener.startFullScreenFromFreeFormAnimation(mffas);
            MiuiFreeformTrackManager miuiFreeformTrackManager = this.mTrackManager;
            if (miuiFreeformTrackManager != null) {
                String stackPackageName = mffas.getStackPackageName();
                String applicationName = mffas.getApplicationName();
                if (mffas.mPinedStartTime != 0) {
                    f = ((float) (System.currentTimeMillis() - mffas.mPinedStartTime)) / 1000.0f;
                }
                miuiFreeformTrackManager.trackSmallWindowPinedQuitEvent(stackPackageName, applicationName, f);
            }
        } else if (mffas.isInMiniFreeFormMode()) {
            this.mGestureListener.startFullScreenFromSmallAnimation(mffas);
            MiuiFreeformTrackManager miuiFreeformTrackManager2 = this.mTrackManager;
            if (miuiFreeformTrackManager2 != null) {
                String stackPackageName2 = mffas.getStackPackageName();
                String applicationName2 = mffas.getApplicationName();
                if (mffas.mPinedStartTime != 0) {
                    f = ((float) (System.currentTimeMillis() - mffas.mPinedStartTime)) / 1000.0f;
                }
                miuiFreeformTrackManager2.trackMiniWindowPinedQuitEvent(stackPackageName2, applicationName2, f);
            }
        }
        mffas.mPinedStartTime = 0L;
        mffas.setInPinMode(false);
    }

    /* renamed from: lambda$startFullscreenFromFreeform$3$com-android-server-wm-MiuiFreeFormGestureController */
    public /* synthetic */ void m1651xb9bbf8b3(MiuiFreeFormActivityStack mffas) {
        if (mffas.isInFreeFormMode()) {
            this.mGestureListener.startFullScreenFromFreeFormAnimation(mffas);
        } else if (mffas.isInMiniFreeFormMode()) {
            this.mGestureListener.startFullScreenFromSmallAnimation(mffas);
        }
    }

    public int getFreeFormWindowMode(int rootStackId) {
        return this.mMiuiFreeFormManagerService.getFreeFormWindowMode(rootStackId);
    }

    public Rect getSmallFreeFormWindowBounds(int rootStackId) {
        return this.mMiuiFreeFormManagerService.getSmallFreeFormWindowBounds(rootStackId);
    }

    public void setMiuiFreeFormTouchExcludeRegion(Region region) {
        this.mGestureListener.setMiuiFreeFormTouchExcludeRegion(region);
    }

    public MiuiFreeFormActivityStack getTopInMiniWindowActivityStack(float x, float y) {
        return this.mMiuiFreeFormManagerService.getTopInMiniWindowActivityStack(x, y);
    }

    public void traverseTask(Task task, Consumer<ActivityRecord> consumer) {
        synchronized (this.mService.mGlobalLock) {
            if (task == null) {
                return;
            }
            boolean z = true;
            for (int activityIndex = task.getChildCount() - 1; activityIndex >= 0; activityIndex--) {
                ActivityRecord activityRecord = task.getChildAt(activityIndex).asActivityRecord();
                if (activityRecord != null) {
                    int orientation = activityRecord.getOrientation();
                    if (orientation != -1 && orientation != 13 && orientation != 10 && orientation != 4 && orientation != 2 && orientation != 14) {
                        if (orientation != 3 && orientation != -2) {
                            if (activityRecord.mVisibleRequested && (activityRecord.getTopFullscreenOpaqueWindow() != null || activityRecord.mChildren.size() == 0)) {
                                MiuiCvwGestureController miuiCvwGestureController = (MiuiCvwGestureController) task.mAtmService.mWindowManager.mMiuiCvwGestureController;
                                MiuiFreeFormActivityStack ffas = this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
                                if (miuiCvwGestureController != null && ffas != null) {
                                    Rect bounds = task.getBounds();
                                    if ((ffas.isLaunchedByCVW() && miuiCvwGestureController.supportLandscapeRatioInConfig(ffas.getStackPackageName()) && miuiCvwGestureController.supportPortraitRatioInConfig(ffas.getStackPackageName())) || (task.mFreeFormLaunchBoundsFromOptions != null && task.mFreeFormLaunchBoundsFromOptions.equals(bounds))) {
                                        return;
                                    }
                                    if (MiuiMultiWindowUtils.isOrientationLandscape(orientation) && miuiCvwGestureController.supportPortraitRatioInConfig(ffas.getStackPackageName())) {
                                        return;
                                    }
                                    if (MiuiMultiWindowUtils.isOrientationPortrait(orientation) && miuiCvwGestureController.supportLandscapeRatioInConfig(ffas.getStackPackageName())) {
                                        return;
                                    }
                                    if (bounds.width() <= bounds.height()) {
                                        z = false;
                                    }
                                    if (z == MiuiMultiWindowUtils.isOrientationLandscape(orientation)) {
                                        return;
                                    }
                                }
                                consumer.accept(activityRecord);
                                return;
                            }
                            MiuiFreeFormActivityStack ffas2 = (MiuiFreeFormActivityStack) this.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(task.getRootTaskId());
                            if (ffas2 != null && ffas2.isFlashBackMode()) {
                                consumer.accept(activityRecord);
                                return;
                            }
                        }
                    }
                    return;
                }
            }
        }
    }

    public void notifyRecentAnimationFinished() {
        this.mGestureListener.notifyRecentAnimationFinished();
    }

    public void prepareSeamlessRotation(Task rootTask) {
        setVisibility(rootTask, false);
    }

    public void finishSeamlessRotation(final Task rootTask) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGestureController$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiFreeFormGestureController.this.m1648xa2f3875f(rootTask);
            }
        });
    }

    /* renamed from: lambda$finishSeamlessRotation$4$com-android-server-wm-MiuiFreeFormGestureController */
    public /* synthetic */ void m1648xa2f3875f(Task rootTask) {
        setVisibility(rootTask, true);
    }

    public void setVisibility(Task rootTask, boolean visible) {
        if (rootTask == null || !rootTask.inFreeformSmallWinMode()) {
            return;
        }
        synchronized (this.mService.mGlobalLock) {
            SurfaceControl sc = rootTask.mSurfaceControl;
            if (sc != null && sc.isValid()) {
                Slog.d(TAG, "set visibility=" + visible + " Tid=" + rootTask.mTaskId);
                rootTask.getPendingTransaction().setVisibility(sc, visible);
            }
        }
    }

    private void registerActivityObserver() {
        this.mService.mAtmService.registerActivityObserver(this.mMiuiActivityObserver, new Intent());
    }

    public void registerFreeformCloudDataObserver() {
        ContentObserver contentObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.wm.MiuiFreeFormGestureController.8
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.d(MiuiFreeFormGestureController.TAG, "receive notification of data update from app");
                MiuiMultiWindowUtils.updateListFromCloud(MiuiFreeFormGestureController.this.mService.mContext);
            }
        };
        this.mService.mContext.getContentResolver().registerContentObserver(Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify"), false, contentObserver);
    }

    public void finishDrawingWindow(WindowState windowstate) {
        this.mGestureListener.finishDrawingWindow(windowstate);
    }

    public boolean isLaunchingSmallFreeFormWindow() {
        List<MiuiFreeFormActivityStack> stackList = this.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
        for (MiuiFreeFormActivityStack mffas : stackList) {
            if (mffas.isInMiniFreeFormMode() && mffas.isLaunchingSmallFreeForm()) {
                return true;
            }
        }
        return false;
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args == null || args.length <= 1) {
            pw.println("dump of freeform gesture");
            pw.println(this.mMiuiFreeFormManagerService.getDumpInfo());
            return;
        }
        String next = args[1];
        if ("0".equals(next)) {
            DEBUG = false;
        } else if (SplitScreenReporter.ACTION_ENTER_SPLIT.equals(next)) {
            DEBUG = true;
        } else {
            pw.println("unknown cmd " + next);
        }
    }
}
