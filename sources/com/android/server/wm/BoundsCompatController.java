package com.android.server.wm;

import android.app.WindowConfiguration;
import android.app.servertransaction.BoundsCompat;
import android.app.servertransaction.BoundsCompatInfoChangeItem;
import android.app.servertransaction.BoundsCompatStub;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.appcompat.ApplicationCompatUtilsStub;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.MiuiAppSizeCompatModeImpl;
import android.view.WindowManager;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class BoundsCompatController extends BoundsCompatControllerStub {
    static int sAlignment;
    private ActivityTaskManagerServiceImpl mATMSImpl;
    private int mAspectRatioPolicy;
    private Rect mBounds;
    private boolean mBoundsCompatEnabled;
    private boolean mDispatchNeeded;
    private Rect mDispatchedBounds;
    private int mDispatchedState;
    private float mFixedAspectRatio = -1.0f;
    private boolean mFixedAspectRatioAvailable;
    private int mGravity;
    private ActivityRecord mOwner;
    private int mState;
    private boolean mWasDisplayCompatInMainDisplay;
    private boolean mWasFixedAspectRatioModeInMainDisplay;

    /* loaded from: classes.dex */
    public @interface AspectRatioPolicy {
        public static final int DEFAULT = 0;
        public static final int FULL_SCREEN_MODE = 1;
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BoundsCompatController> {

        /* compiled from: BoundsCompatController$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BoundsCompatController INSTANCE = new BoundsCompatController();
        }

        public BoundsCompatController provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BoundsCompatController provideNewInstance() {
            return new BoundsCompatController();
        }
    }

    public void initBoundsCompatController(ActivityRecord owner) {
        this.mOwner = owner;
        this.mATMSImpl = ActivityTaskManagerServiceImpl.getInstance();
    }

    public void clearSizeCompatMode(Task task, final boolean isClearConfig, final ActivityRecord excluded) {
        task.mDisplayCompatAvailable = false;
        task.forAllActivities(new Consumer() { // from class: com.android.server.wm.BoundsCompatController$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                BoundsCompatController.lambda$clearSizeCompatMode$0(excluded, isClearConfig, (ActivityRecord) obj);
            }
        });
    }

    public static /* synthetic */ void lambda$clearSizeCompatMode$0(ActivityRecord excluded, boolean isClearConfig, ActivityRecord ar) {
        if (ar == excluded) {
            return;
        }
        if (!isClearConfig) {
            ar.clearCompatDisplayInsets();
            return;
        }
        ar.clearSizeCompatMode();
        if (!ar.attachedToProcess()) {
            return;
        }
        try {
            ar.mBoundsCompatControllerStub.interceptScheduleTransactionIfNeeded((ClientTransactionItem) null);
        } catch (RemoteException e) {
        }
    }

    private void adjustBoundsIfNeeded(Configuration resolvedConfig, Configuration parentConfig, Rect sizeCompatBounds, float sizeCompatScale) {
        int newSizeCompatLeft;
        Rect parentBounds = parentConfig.windowConfiguration.getBounds();
        Rect parentAppBounds = parentConfig.windowConfiguration.getAppBounds();
        Rect resolvedBounds = resolvedConfig.windowConfiguration.getBounds();
        Rect resolvedAppBounds = resolvedConfig.windowConfiguration.getAppBounds();
        int isLandscape = resolvedConfig.orientation == 2 ? 1 : 0;
        if (isDisplayCompatModeEnabled()) {
            resolvedBounds.set(resolvedAppBounds);
            if (isLandscape == 0) {
                int screenTopInset = parentAppBounds.top - parentBounds.top;
                if (screenTopInset != 0) {
                    resolvedBounds.offsetTo(resolvedBounds.left, screenTopInset);
                    resolvedAppBounds.offsetTo(resolvedAppBounds.left, screenTopInset);
                    if (sizeCompatBounds != null) {
                        sizeCompatBounds.offsetTo(sizeCompatBounds.left, screenTopInset);
                    }
                }
                int newLeft = getHorizontalOffsetTo(0, parentAppBounds, resolvedAppBounds);
                if (resolvedAppBounds.left != newLeft) {
                    resolvedBounds.offsetTo(newLeft, resolvedBounds.top);
                    resolvedAppBounds.offsetTo(newLeft, resolvedAppBounds.top);
                }
                if (sizeCompatBounds != null && sizeCompatBounds.left != (newSizeCompatLeft = getHorizontalOffsetTo(0, parentAppBounds, sizeCompatBounds))) {
                    sizeCompatBounds.offsetTo(newSizeCompatLeft, sizeCompatBounds.top);
                    return;
                }
                return;
            }
            int offsetY = getVerticalOffset(0, parentAppBounds, resolvedAppBounds, sizeCompatScale, this.mOwner.mDisplayContent);
            if (offsetY != 0) {
                resolvedBounds.offset(0, offsetY);
                resolvedAppBounds.offset(0, offsetY);
                if (sizeCompatBounds != null) {
                    sizeCompatBounds.offset(0, offsetY);
                }
            }
        }
    }

    private void applyPolicyIfNeeded(ActivityInfo info) {
        Task task;
        this.mFixedAspectRatioAvailable = false;
        float fixedAspectRatio = -1.0f;
        if (!this.mOwner.mAtmService.mWindowManager.mPolicy.isDisplayFolded() && (task = this.mOwner.getTask()) != null && task.realActivity != null && !task.inMultiWindowMode()) {
            fixedAspectRatio = this.mATMSImpl.getAspectRatio(info.packageName);
            this.mGravity = this.mATMSImpl.getAspectGravity(info.packageName);
        }
        if (Float.compare(this.mFixedAspectRatio, fixedAspectRatio) != 0) {
            this.mFixedAspectRatio = fixedAspectRatio;
            if (fixedAspectRatio == MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mAspectRatioPolicy = 1;
            } else {
                this.mAspectRatioPolicy = 0;
            }
        }
        if (fixedAspectRatio != -1.0f) {
            if (ActivityTaskManagerServiceStub.hasDefinedAspectRatio(fixedAspectRatio)) {
                this.mFixedAspectRatioAvailable = true;
                return;
            }
            return;
        }
        this.mAspectRatioPolicy = 0;
    }

    private static int getHorizontalOffsetTo(int modeShift, Rect parentAppBounds, Rect resolvedAppBounds) {
        int i = sAlignment;
        if (i != 0) {
            if (Alignment.isHorizontalLeft(i, modeShift)) {
                return 0;
            }
            if (Alignment.isHorizontalRight(sAlignment, modeShift)) {
                return parentAppBounds.right - resolvedAppBounds.width();
            }
        }
        return resolvedAppBounds.left;
    }

    private static int getVerticalCenterOffset(int viewportH, int contentH) {
        return (int) (((viewportH - contentH) + 1) * 0.5f);
    }

    private static int getVerticalOffset(int modeShift, Rect parentAppBounds, Rect resolvedAppBounds, float sizeCompatScale, DisplayContent dc) {
        int i = sAlignment;
        if (i != 0) {
            if (Alignment.isVerticalTop(i, modeShift)) {
                if (dc == null) {
                    return 0;
                }
                int offset = dc.getDisplayPolicy().getStatusBarHeight(dc.mDisplayFrames);
                return offset;
            }
            int offset2 = sAlignment;
            if (Alignment.isVerticalBottom(offset2, modeShift)) {
                return parentAppBounds.bottom - resolvedAppBounds.height();
            }
        }
        return getVerticalCenterOffset(parentAppBounds.height(), (int) (resolvedAppBounds.height() * sizeCompatScale));
    }

    private static boolean isLandscape(Configuration config) {
        return config.orientation == 2;
    }

    void addCallbackIfNeeded(ClientTransaction transaction) {
        if (this.mDispatchNeeded) {
            this.mDispatchNeeded = false;
            transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mDispatchedState, this.mDispatchedBounds));
        }
    }

    void adjustWindowParamsIfNeededLocked(WindowState win, WindowManager.LayoutParams attrs) {
        if (isFullScreen()) {
            if (win.mAttrs.layoutInDisplayCutoutMode == 0 || attrs.layoutInDisplayCutoutMode == 0) {
                win.mAttrs.layoutInDisplayCutoutMode = 1;
                attrs.layoutInDisplayCutoutMode = 1;
                attrs.flags |= 256;
            }
        }
    }

    void clearAppBoundsIfNeeded(Configuration resolvedConfig) {
        Rect resolvedAppBounds = resolvedConfig.windowConfiguration.getAppBounds();
        if (resolvedAppBounds != null && BoundsCompatStub.get().isBoundsCompatEnabled(this.mState)) {
            resolvedAppBounds.setEmpty();
        }
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("BoundsCompatInfo:");
        pw.print(" mState=0x" + Integer.toHexString(this.mState));
        pw.print(" mCompatBounds=" + this.mBounds);
        if (this.mDispatchNeeded) {
            pw.print(" mDispatchNeeded=true");
        }
        if (this.mState != this.mDispatchedState || !this.mBounds.equals(this.mDispatchedBounds)) {
            pw.print(" mDispatchedState=" + Integer.toHexString(this.mDispatchedState));
            pw.print(" mDispatchedBounds=" + this.mDispatchedBounds);
        }
        if (this.mFixedAspectRatio != -1.0f) {
            pw.print(" mFixedAspectRatio=" + this.mFixedAspectRatio);
        }
        pw.println();
        if (this.mAspectRatioPolicy != 0) {
            pw.print(prefix);
            pw.print(" mAspectRatioPolicy=");
            String str = aspectRatioPolicyToString(this.mAspectRatioPolicy);
            pw.print(str);
            pw.println();
        }
    }

    private String aspectRatioPolicyToString(int policy) {
        switch (policy) {
            case 0:
                return "Default";
            case 1:
                return "FullScreen";
            default:
                return Integer.toString(policy);
        }
    }

    void dumpBounds(PrintWriter pw, String prefix, String title, WindowConfiguration winConfig) {
        pw.print(prefix + title);
        pw.print(" Bounds=");
        Rect bounds = winConfig.getBounds();
        pw.print("(" + bounds.left + "," + bounds.top + ")");
        pw.print("(" + bounds.width() + "x" + bounds.height() + ")");
        Rect appBounds = winConfig.getAppBounds();
        if (appBounds != null) {
            pw.print(" AppBounds=");
            pw.print("(" + appBounds.left + "," + appBounds.top + ")");
            pw.print("(" + appBounds.width() + "x" + appBounds.height() + ")");
        }
        pw.println();
    }

    public boolean interceptScheduleTransactionIfNeeded(ClientTransactionItem originalTransactionItem) throws RemoteException {
        if (this.mDispatchNeeded) {
            this.mDispatchNeeded = false;
            ClientTransaction transaction = ClientTransaction.obtain(this.mOwner.app.getThread(), this.mOwner.token);
            transaction.addCallback(BoundsCompatInfoChangeItem.obtain(this.mDispatchedState, this.mDispatchedBounds));
            if (originalTransactionItem != null) {
                transaction.addCallback(originalTransactionItem);
            }
            this.mOwner.mAtmService.getLifecycleManager().scheduleTransaction(transaction);
            return true;
        }
        return false;
    }

    boolean isBoundsCompatEnabled() {
        return this.mBoundsCompatEnabled;
    }

    boolean isDisplayCompatAvailable(Task task) {
        if (task.mAtmService.mWindowManager.mPolicy.isDisplayFolded()) {
            if (task.getDisplayId() != 0 || !task.isActivityTypeStandard() || task.inMultiWindowMode() || ActivityTaskManagerServiceStub.isForcedResizeableDisplayCompatPolicy(task.mDisplayCompatPolicy)) {
                return false;
            }
            if (ActivityTaskManagerServiceStub.isForcedUnresizeableDisplayCompatPolicy(task.mDisplayCompatPolicy)) {
                return true;
            }
            return (task.mResizeMode == 2 || task.mResizeMode == 1) ? false : true;
        } else if (task.mDisplayCompatAvailable && task.inMultiWindowMode()) {
            return false;
        } else {
            return task.mDisplayCompatAvailable;
        }
    }

    public boolean isDisplayCompatModeEnabled() {
        return BoundsCompat.getInstance().isDisplayCompatModeEnabled(this.mState);
    }

    public boolean isFixedAspectRatioAvailable() {
        return this.mFixedAspectRatioAvailable;
    }

    public boolean isFixedAspectRatioEnabled() {
        return BoundsCompat.getInstance().isFixedAspectRatioModeEnabled(this.mState);
    }

    public boolean isFullScreen() {
        if (this.mAspectRatioPolicy == 1) {
            return true;
        }
        return false;
    }

    public void makeDisplayCompatModeEnabled() {
        this.mState |= 4;
    }

    public void prepareBoundsCompat() {
        this.mState = 0;
        if (this.mOwner.isActivityTypeStandardOrUndefined()) {
            applyPolicyIfNeeded(this.mOwner.info);
        }
    }

    boolean inMiuiSizeCompatMode() {
        Rect rect = this.mDispatchedBounds;
        return rect != null && !rect.isEmpty();
    }

    boolean shouldNotCreateCompatDisplayInsets() {
        boolean inMiuiSizeCompatMode = !this.mOwner.mAtmService.mForceResizableActivities && inMiuiSizeCompatMode();
        return inMiuiSizeCompatMode || shouldCreateCompatDisplayInsetsWithContinuity();
    }

    boolean areBoundsLetterboxed() {
        return inMiuiSizeCompatMode() && this.mOwner.areBoundsLetterboxed();
    }

    private boolean shouldCreateCompatDisplayInsetsWithContinuity() {
        if (!ApplicationCompatUtilsStub.get().isContinuityEnabled() || this.mOwner.task == null) {
            return false;
        }
        return this.mATMSImpl.getMetaDataBoolean(this.mOwner.packageName, "miui.supportAppContinuity") || this.mOwner.task.mDisplayCompatPolicy == 4 || this.mOwner.task.mDisplayCompatPolicy == 7 || this.mOwner.task.mDisplayCompatPolicy == 6 || this.mOwner.task.mDisplayCompatPolicy == 3;
    }

    boolean shouldShowLetterboxUi() {
        Task task;
        ActivityRecord activityRecord = this.mOwner;
        if (activityRecord != null && activityRecord.inMiuiSizeCompatMode() && (task = this.mOwner.getTask()) != null) {
            return taskContainsActivityRecord(task);
        }
        return false;
    }

    private boolean taskContainsActivityRecord(Task task) {
        for (int j = 0; j < task.getChildCount(); j++) {
            if (task.isLeafTask()) {
                ActivityRecord ar = task.getChildAt(j).asActivityRecord();
                if (ar != null && ar.isVisible()) {
                    ActivityRecord activityRecord = this.mOwner;
                    if (ar == activityRecord) {
                        return true;
                    }
                    int currentOrientation = getRequestedConfigurationOrientation(activityRecord);
                    int bottomOrientation = getRequestedConfigurationOrientation(ar);
                    return (currentOrientation == 0 || bottomOrientation == 0 || currentOrientation == bottomOrientation) ? false : true;
                }
            } else {
                for (int i = task.getChildCount() - 1; i >= 0; i--) {
                    Task t = task.getChildAt(i).asTask();
                    if (t != null && task.isVisible()) {
                        return taskContainsActivityRecord(t);
                    }
                }
                continue;
            }
        }
        return false;
    }

    void resolveBoundsCompat(Configuration resolvedConfig, Configuration parentConfig, Rect sizeCompatBounds, float sizeCompatScale) {
        boolean isBoundsCompatEnabled = BoundsCompat.getInstance().isBoundsCompatEnabled(this.mState);
        this.mBoundsCompatEnabled = isBoundsCompatEnabled;
        if (isBoundsCompatEnabled) {
            adjustBoundsIfNeeded(resolvedConfig, parentConfig, sizeCompatBounds, sizeCompatScale);
            if (this.mBounds == null) {
                this.mBounds = new Rect();
            }
            this.mBounds.set(resolvedConfig.windowConfiguration.getBounds());
            if (this.mDispatchedBounds == null) {
                this.mDispatchedBounds = new Rect();
            }
            if (!this.mDispatchedBounds.equals(this.mBounds)) {
                this.mDispatchedBounds.set(this.mBounds);
                this.mDispatchNeeded = true;
            }
        } else if (BoundsCompat.getInstance().isBoundsCompatEnabled(this.mDispatchedState)) {
            this.mBounds.setEmpty();
            this.mDispatchedBounds.setEmpty();
            this.mDispatchNeeded = true;
        }
        int i = this.mState;
        if (i != this.mDispatchedState) {
            this.mDispatchedState = i;
            this.mDispatchNeeded = true;
        }
        if (!this.mOwner.mAtmService.mWindowManager.mPolicy.isDisplayFolded()) {
            this.mWasFixedAspectRatioModeInMainDisplay = isFixedAspectRatioEnabled();
            this.mWasDisplayCompatInMainDisplay = isDisplayCompatModeEnabled();
        }
    }

    public Rect getDispatchedBounds() {
        return this.mDispatchedBounds;
    }

    public void updateDisplayCompatModeAvailableIfNeeded(Task task) {
        int policy;
        if (task.realActivity != null && task.mDisplayCompatPolicy != (policy = this.mATMSImpl.getPolicy(task.realActivity.getPackageName()))) {
            task.mDisplayCompatPolicy = policy;
        }
        boolean isAvailable = isDisplayCompatAvailable(task);
        if (task.mDisplayCompatAvailable != isAvailable) {
            task.mDisplayCompatAvailable = isAvailable;
            if (!isAvailable) {
                clearSizeCompatMode(task, false);
            }
        }
    }

    boolean adaptCompatConfiguration(Configuration parentConfiguration, Configuration resolvedConfig, DisplayContent dc) {
        if (canUseFixedAspectRatio(parentConfiguration)) {
            this.mOwner.clearSizeCompatModeWithoutConfigChange();
            Rect parentAppBounds = parentConfiguration.windowConfiguration.getAppBounds();
            Rect compatBounds = BoundsCompat.getInstance().computeCompatBounds(this.mFixedAspectRatio, parentConfiguration, getRequestedConfigurationOrientation(this.mOwner), this.mGravity);
            Rect compatAppBounds = new Rect(compatBounds);
            if (compatBounds.top < parentAppBounds.top) {
                compatAppBounds.top = parentAppBounds.top;
            }
            if (compatBounds.bottom > parentAppBounds.bottom) {
                compatAppBounds.bottom = parentAppBounds.bottom;
            }
            if (resolvedConfig.densityDpi == 0) {
                resolvedConfig.densityDpi = parentConfiguration.densityDpi;
            }
            resolvedConfig.windowConfiguration.setBounds(compatBounds);
            resolvedConfig.windowConfiguration.setAppBounds(compatAppBounds);
            boolean shouldUseMaxBoundsFullscreen = MiuiAppSizeCompatModeImpl.getInstance().shouldUseMaxBoundsFullscreen(this.mOwner.getName());
            if (!shouldUseMaxBoundsFullscreen) {
                resolvedConfig.windowConfiguration.setMaxBounds(compatBounds);
            }
            BoundsCompatUtils.getInstance().adaptCompatBounds(resolvedConfig, dc);
            if (this.mFixedAspectRatioAvailable) {
                this.mState |= 8;
                return true;
            }
            return true;
        }
        return false;
    }

    private int getRequestedConfigurationOrientation(ActivityRecord ar) {
        int requestedOrientation;
        int sensorRotation;
        boolean useOriginRequestOrientation = ar.mOriginRequestOrientation == ar.mOriginScreenOrientation || ar.mOriginRequestOrientation != -2;
        if (useOriginRequestOrientation) {
            requestedOrientation = ar.mOriginRequestOrientation;
        } else {
            requestedOrientation = ar.mOriginScreenOrientation;
        }
        if (requestedOrientation == 5) {
            if (ar.mDisplayContent != null) {
                return ar.mDisplayContent.getNaturalOrientation();
            }
        } else if (requestedOrientation == 14) {
            return ar.getConfiguration().orientation;
        } else {
            if (ActivityInfo.isFixedOrientationLandscape(requestedOrientation)) {
                return 2;
            }
            if (ActivityInfo.isFixedOrientationPortrait(requestedOrientation)) {
                return 1;
            }
            if (requestedOrientation == 4 && ar.mDisplayContent != null) {
                DisplayRotation displayRotation = ar.mDisplayContent.getDisplayRotation();
                WindowOrientationListener listener = displayRotation != null ? displayRotation.getOrientationListener() : null;
                if (listener != null) {
                    sensorRotation = listener.getProposedRotation();
                } else {
                    sensorRotation = -1;
                }
                int sensorOrientation = rotationToOrientation(sensorRotation);
                if (sensorOrientation != -1) {
                    return sensorOrientation;
                }
            }
        }
        return 0;
    }

    private int rotationToOrientation(int rotation) {
        switch (rotation) {
            case 0:
            case 2:
                return 1;
            case 1:
            case 3:
                return 2;
            default:
                return -1;
        }
    }

    private boolean canUseFixedAspectRatio(Configuration parentConfiguration) {
        if (!this.mFixedAspectRatioAvailable || isFullScreen() || this.mOwner.getUid() < 10000 || ActivityTaskManagerServiceStub.get().shouldNotApplyAspectRatio()) {
            return false;
        }
        return true;
    }

    /* loaded from: classes.dex */
    public static final class Alignment {
        private static final int HORIZONTAL_LEFT = 4;
        private static final int HORIZONTAL_MASK = 12;
        private static final int HORIZONTAL_RIGHT = 8;
        private static final int VERTICAL_BOTTOM = 2;
        private static final int VERTICAL_MASK = 3;
        private static final int VERTICAL_TOP = 1;

        private Alignment() {
        }

        public static boolean isHorizontalLeft(int alignment, int modeShift) {
            return ((alignment >> modeShift) & 12) == 4;
        }

        public static boolean isHorizontalRight(int alignment, int modeShift) {
            return ((alignment >> modeShift) & 12) == 8;
        }

        public static boolean isVerticalBottom(int alignment, int modeShift) {
            return ((alignment >> modeShift) & 3) == 2;
        }

        public static boolean isVerticalTop(int alignment, int modeShift) {
            return ((alignment >> modeShift) & 3) == 1;
        }
    }
}
