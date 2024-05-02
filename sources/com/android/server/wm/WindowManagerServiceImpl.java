package com.android.server.wm;

import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IWallpaperManagerCallback;
import android.app.IWindowSecureChangeListener;
import android.app.WallpaperColors;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.MiuiDisplayMetrics;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IWindowAnimationFinishedCallback;
import android.view.SurfaceControl;
import android.view.WindowManager;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.SmartPowerService;
import com.android.server.wallpaper.WallpaperManagerInternal;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.miui.whetstone.PowerKeeperPolicy;
import com.xiaomi.screenprojection.IMiuiScreenProjectionStub;
import java.io.Closeable;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import miui.io.IOUtils;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class WindowManagerServiceImpl implements WindowManagerServiceStub {
    private static final String BUILD_DENSITY_PROP = "ro.sf.lcd_density";
    private static final String CLIENT_DIMMER_NAME = "ClientDimmerForAppPair";
    private static final String MIUI_RESOLUTION = "persist.sys.miui_resolution";
    public static final int MSG_SERVICE_INIT_AFTER_BOOT = 107;
    public static final int MSG_UPDATE_BLUR_WALLPAPER = 106;
    public static final int SCREEN_SHARE_PROTECTION_OPEN = 1;
    public static final int SCREEN_SHARE_PROTECTION_WITH_BLUR = 1;
    private static final String TAG = "WindowManagerService";
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    boolean isFpClientOn;
    AppOpsManager mAppOps;
    ActivityTaskManagerService mAtmService;
    private List<String> mBlackList;
    private Bitmap mBlurWallpaperBmp;
    private BroadcastReceiver mBootCompletedReceiver;
    private CameraManager mCameraManager;
    Context mContext;
    DisplayContent mDisplayContent;
    DisplayManagerInternal mDisplayManagerInternal;
    private WindowManagerGlobalLock mGlobalLock;
    private boolean mIsResolutionChanged;
    MiuiContrastOverlayStub mMiuiContrastOverlay;
    private int mMiuiDisplayDensity;
    private int mMiuiDisplayHeight;
    private int mMiuiDisplayWidth;
    private boolean mPendingSwitchResolution;
    private int mPhysicalDensity;
    private int mPhysicalHeight;
    private int mPhysicalWidth;
    private boolean mRunningRecentsAnimation;
    private boolean mSupportActiveModeSwitch;
    private boolean mSupportSwitchResolutionFeature;
    IWindowAnimationFinishedCallback mUiModeAnimFinishedCallback;
    private IWallpaperManagerCallback mWallpaperCallback;
    WindowManagerService mWmService;
    private static String[] FORCE_ORI_LIST = {"com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity", "com.tencent.mm/com.tencent.mm.plugin.multitalk.ui.MultiTalkMainUI"};
    private static String[] FORCE_ORI_DEVICES_LIST = {"lithium", "chiron", "polaris"};
    private static String CUR_DEVICE = Build.DEVICE;
    private static List<String> mProjectionBlackList = new ArrayList();
    public static String MM = "com.tencent.mm";
    public static String QQ = "com.tencent.mobileqq";
    public static String GOOGLE = "com.google.android.dialer";
    public static String SECURITY = ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME;
    public static String MM_FLOATING = "com.tencent.mm/.FloatingWindow";
    public static String QQ_FLOATING = "com.tencent.mobileqq/.FloatingWindow";
    public static String GOOGLE_FLOATING = "com.google.android.dialer/.FloatingWindow";
    public static String SECURITY_FLOATING = "com.miui.securitycenter/.FloatingWindow";
    private static final boolean SUPPPORT_CLOUD_DIM = FeatureParser.getBoolean("support_cloud_dim", false);
    private HashMap<String, Integer> mDimUserTimeoutMap = new HashMap<>();
    private HashMap<String, Boolean> mDimNeedAssistMap = new HashMap<>();
    private PowerKeeperPolicy mPowerKeeperPolicy = null;
    private SparseArray<Float> mTaskIdScreenBrightnessOverrides = new SparseArray<>();
    boolean IS_CTS_MODE = false;
    boolean HAS_SCREEN_SHOT = false;
    private boolean mSplitMode = false;
    private Set mOpeningCameraID = new HashSet();
    private final Uri mDarkModeEnable = Settings.System.getUriFor("ui_night_mode");
    private final Uri mDarkModeContrastEnable = Settings.System.getUriFor("dark_mode_contrast_enable");
    private final Uri mContrastAlphaUri = Settings.System.getUriFor("contrast_alpha");
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.wm.WindowManagerServiceImpl.3
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            WindowManagerServiceImpl.this.mOpeningCameraID.remove(Integer.valueOf(id));
            if (WindowManagerServiceImpl.this.mOpeningCameraID.size() == 0) {
                DisplayContent displayContent = WindowManagerServiceImpl.this.mDisplayContent;
            }
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            WindowManagerServiceImpl.this.mOpeningCameraID.add(Integer.valueOf(id));
        }
    };
    final ArrayList<IWindowSecureChangeListener> mSecureChangeListeners = new ArrayList<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowManagerServiceImpl> {

        /* compiled from: WindowManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowManagerServiceImpl INSTANCE = new WindowManagerServiceImpl();
        }

        public WindowManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WindowManagerServiceImpl provideNewInstance() {
            return new WindowManagerServiceImpl();
        }
    }

    /* loaded from: classes.dex */
    private final class WallpaperDeathMonitor implements IBinder.DeathRecipient {
        final int mDisplayId;
        final IBinder mIBinder;

        WallpaperDeathMonitor(IBinder binder, int displayId) {
            WindowManagerServiceImpl.this = r1;
            this.mDisplayId = displayId;
            this.mIBinder = binder;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            WindowManagerServiceImpl.this.mWmService.removeWindowToken(this.mIBinder, this.mDisplayId);
            this.mIBinder.unlinkToDeath(this, 0);
        }
    }

    public static WindowManagerServiceImpl getInstance() {
        return (WindowManagerServiceImpl) WindowManagerServiceStub.get();
    }

    private void registerBootCompletedReceiver() {
        this.mWallpaperCallback = new IWallpaperManagerCallback.Stub() { // from class: com.android.server.wm.WindowManagerServiceImpl.1
            public void onWallpaperChanged() throws RemoteException {
                WindowManagerServiceImpl.this.mWmService.mH.removeMessages(106);
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage(106);
            }

            public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) throws RemoteException {
                WindowManagerServiceImpl.this.mWmService.mH.removeMessages(106);
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage(106);
            }
        };
        this.mBootCompletedReceiver = new BroadcastReceiver() { // from class: com.android.server.wm.WindowManagerServiceImpl.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                WindowManagerServiceImpl.this.mWmService.mH.sendEmptyMessage((int) WindowManagerServiceImpl.MSG_SERVICE_INIT_AFTER_BOOT);
            }
        };
        IntentFilter bootCompletedFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBootCompletedReceiver, bootCompletedFilter);
    }

    public void initAfterBoot() {
        if (this.mBlurWallpaperBmp == null) {
            updateBlurWallpaperBmp();
        }
    }

    public Bitmap getBlurWallpaperBmp() {
        Bitmap bitmap = this.mBlurWallpaperBmp;
        if (bitmap == null) {
            this.mWmService.mH.removeMessages(106);
            this.mWmService.mH.sendEmptyMessage(106);
            Slog.d(TAG, "mBlurWallpaperBmp is null, wait to update");
            return null;
        } else if (bitmap == null) {
            return null;
        } else {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    public void updateBlurWallpaperBmp() {
        WallpaperManagerInternal wpMgr = (WallpaperManagerInternal) LocalServices.getService(WallpaperManagerInternal.class);
        if (wpMgr == null) {
            Slog.w(TAG, "WallpaperManagerInternal is null");
            return;
        }
        ParcelFileDescriptor fd = null;
        try {
            try {
                fd = wpMgr.getBlurWallpaper(this.mWallpaperCallback);
                if (fd == null) {
                    Slog.w(TAG, "getWallpaper, fd is null");
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    this.mBlurWallpaperBmp = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
                    if (MiuiSizeCompatService.DEBUG) {
                        Slog.i(TAG, "decodeFileDescriptor, mBlurWallpaperBmp = " + this.mBlurWallpaperBmp);
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "getWallpaper wrong", e);
            }
        } finally {
            IOUtils.closeQuietly((Closeable) null);
        }
    }

    public boolean executeShellCommand(PrintWriter pw, String[] args, int opti, String opt) {
        String[] str;
        if (opti < args.length) {
            str = new String[args.length - opti];
        } else {
            str = new String[0];
        }
        System.arraycopy(args, opti, str, 0, args.length - opti);
        pw.println("WindowManagerServiceImpl:executeShellCommand opti = " + opti + " opt = " + opt);
        try {
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return ActivityTaskManagerServiceStub.get().executeShellCommand(opt, str, pw);
    }

    public void init(WindowManagerService wms, Context context) {
        this.mContext = context;
        this.mWmService = wms;
        this.mAppOps = wms.mAppOps;
        this.mAtmService = this.mWmService.mAtmService;
        this.mGlobalLock = this.mWmService.mGlobalLock;
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mSupportSwitchResolutionFeature = isSupportSwitchResoluton();
        checkDDICSupportAndInitPhysicalSize();
        CameraManager cameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mCameraManager = cameraManager;
        cameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, UiThread.getHandler());
        registerBootCompletedReceiver();
        registerMiuiOptimizationObserver(this.mContext);
        AppOpsManager.OnOpChangedListener listener = new WindowOpChangedListener(this.mWmService, context);
        this.mAppOps.startWatchingMode(10041, (String) null, listener);
        this.mPowerKeeperPolicy = PowerKeeperPolicy.getInstance();
        this.mBlackList = Arrays.asList(this.mContext.getResources().getStringArray(285409381));
    }

    public void adjustWindowParams(WindowManager.LayoutParams attrs, String packageName, int uid) {
        if (attrs == null) {
            return;
        }
        if (uid != 1000 && ((attrs.flags & 524288) != 0 || (attrs.flags & SmartPowerPolicyManager.WHITE_LIST_TYPE_PROVIDER_CLOUDCONTROL) != 0)) {
            ApplicationInfo appInfo = null;
            try {
                appInfo = this.mContext.getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(uid));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            int appUid = appInfo == null ? uid : appInfo.uid;
            int mode = this.mAppOps.noteOpNoThrow(10020, appUid, packageName, (String) null, "WindowManagerServiceImpl#adjustWindowParams");
            if (mode != 0) {
                attrs.flags &= -524289;
                attrs.flags &= -4194305;
                Slog.i(TAG, "MIUILOG- Show when locked PermissionDenied pkg : " + packageName + " uid : " + appUid);
            }
        }
        adjustFindDeviceAttrs(uid, attrs, packageName);
        if ((attrs.flags & 4096) != 0 && attrs.type <= 99) {
            attrs.flags &= -4097;
        }
    }

    /* loaded from: classes.dex */
    private static class WindowOpChangedListener implements AppOpsManager.OnOpChangedListener {
        private static final int SCREEN_SHARE_PROTECTION_OPEN = 1;
        private Context mContext;
        private final WindowManagerService mWmService;

        public WindowOpChangedListener(WindowManagerService mWmService, Context context) {
            this.mWmService = mWmService;
            this.mContext = context;
        }

        @Override // android.app.AppOpsManager.OnOpChangedListener
        public void onOpChanged(String op, final String packageName) {
            boolean z = true;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_share_protection_on", 0, -2) != 1) {
                z = false;
            }
            boolean isScreenShareProtection = z;
            if (isScreenShareProtection && AppOpsManager.opToPublicName(10041).equals(op)) {
                try {
                    ApplicationInfo appInfo = this.mWmService.mContext.getPackageManager().getApplicationInfo(packageName, 0);
                    final int newMode = this.mWmService.mAppOps.noteOpNoThrow(10041, appInfo.uid, packageName);
                    synchronized (this.mWmService.mGlobalLock) {
                        this.mWmService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$WindowOpChangedListener$$ExternalSyntheticLambda0
                            @Override // java.util.function.Consumer
                            public final void accept(Object obj) {
                                WindowManagerServiceImpl.WindowOpChangedListener.lambda$onOpChanged$0(packageName, newMode, (WindowState) obj);
                            }
                        }, false);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public static /* synthetic */ void lambda$onOpChanged$0(String packageName, int newMode, WindowState w) {
            if (TextUtils.equals(packageName, w.getOwningPackage()) && w.mWinAnimator != null && w.mWinAnimator.mSurfaceController != null && w.mWinAnimator.mSurfaceController.mSurfaceControl != null) {
                SurfaceControl sc = w.mWinAnimator.mSurfaceController.mSurfaceControl;
                if (newMode == 1) {
                    sc.setScreenProjection(1);
                } else {
                    sc.setScreenProjection(0);
                }
                Slog.i(WindowManagerServiceImpl.TAG, "MIUILOG- Adjust ShareProjectFlag for:" + packageName + " newMode: " + newMode);
            }
        }
    }

    public boolean isAllowedDisableKeyguard(int uid) {
        if (UserHandle.getAppId(uid) < 10000) {
            return true;
        }
        String[] packages = null;
        try {
            packages = AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
        }
        if (packages == null || packages.length == 0) {
            return true;
        }
        int mode = this.mAppOps.checkOpNoThrow(10020, uid, packages[0]);
        if (mode == 0) {
            return true;
        }
        Slog.i(TAG, "MIUILOG- DisableKeyguard PermissionDenied uid : " + uid);
        return false;
    }

    static int getForceOrientation(ActivityRecord atoken, int lastOrientation) {
        WindowState win;
        String[] strArr;
        if (needForceOrientation() && (win = atoken.findMainWindow()) != null && (strArr = FORCE_ORI_LIST) != null) {
            for (String name : strArr) {
                if (name.equals(win.getAttrs().getTitle())) {
                    return 7;
                }
            }
        }
        return lastOrientation;
    }

    private static boolean needForceOrientation() {
        String[] strArr;
        for (String device : FORCE_ORI_DEVICES_LIST) {
            if (device.equals(CUR_DEVICE)) {
                return true;
            }
        }
        return false;
    }

    private void adjustFindDeviceAttrs(int uid, WindowManager.LayoutParams attrs, String packageName) {
        addShowOnFindDeviceKeyguardAttrsIfNecessary(attrs, packageName);
        removeFindDeviceKeyguardFlagsIfNecessary(uid, attrs, packageName);
    }

    private void addShowOnFindDeviceKeyguardAttrsIfNecessary(WindowManager.LayoutParams attrs, String packageName) {
        if (!TextUtils.equals("com.google.android.dialer", packageName) || Settings.Global.getInt(this.mContext.getContentResolver(), "com.xiaomi.system.devicelock.locked", 0) == 0) {
            return;
        }
        attrs.format = -1;
        attrs.layoutInDisplayCutoutMode = 0;
        attrs.extraFlags |= 4096;
    }

    private void removeFindDeviceKeyguardFlagsIfNecessary(int uid, WindowManager.LayoutParams attrs, String packageName) {
        if ((attrs.extraFlags & 2048) == 0 && (attrs.extraFlags & 4096) == 0) {
            return;
        }
        if (isFindDeviceFlagUsePermitted(uid, packageName) && ((attrs.extraFlags & 2048) == 0 || "com.xiaomi.finddevice".equals(packageName))) {
            return;
        }
        attrs.extraFlags &= -2049;
        attrs.extraFlags &= -4097;
    }

    private boolean isFindDeviceFlagUsePermitted(int uid, String packageName) {
        IPackageManager pm;
        if (!TextUtils.isEmpty(packageName) && (pm = AppGlobals.getPackageManager()) != null) {
            if (pm.checkSignatures("android", packageName) == 0) {
                return true;
            }
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0L, UserHandle.getUserId(uid));
            if (ai != null) {
                if ((ai.flags & 1) != 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        if (code == 255) {
            data.enforceInterface("android.view.IWindowManager");
            return switchResolution(data, reply, flags);
        }
        return false;
    }

    boolean switchResolution(Parcel data, Parcel reply, int flags) {
        int displayId = data.readInt();
        int width = data.readInt();
        int height = data.readInt();
        if (displayId != 0) {
            throw new IllegalArgumentException("Can only set the default display");
        }
        long ident = Binder.clearCallingIdentity();
        try {
            switchResolutionInternal(width, height);
            Binder.restoreCallingIdentity(ident);
            reply.writeNoException();
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private void switchResolutionInternal(int width, int height) {
        synchronized (this.mGlobalLock) {
            if (this.mPendingSwitchResolution) {
                return;
            }
            int density = calcDensityAndUpdateForceDensityIfNeed(width);
            Slog.i(TAG, "start switching resolution, width:" + width + ",height:" + height + ",density:" + density);
            this.mMiuiDisplayWidth = width;
            this.mMiuiDisplayHeight = height;
            this.mMiuiDisplayDensity = density;
            this.mPendingSwitchResolution = true;
            this.mDisplayContent.setRoundedCornerOverlaysCanScreenShot(true);
            SystemProperties.set("persist.sys.miui_resolution", this.mMiuiDisplayWidth + "," + this.mMiuiDisplayHeight + "," + this.mMiuiDisplayDensity);
            ActivityTaskManagerService.H h = this.mWmService.mAtmService.mH;
            final ActivityManagerInternal activityManagerInternal = this.mWmService.mAmInternal;
            Objects.requireNonNull(activityManagerInternal);
            h.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    activityManagerInternal.notifyResolutionChanged();
                }
            });
            SmartPowerService.getInstance().onDisplaySwitchResolutionLocked(width, height, density);
            updateScreenResolutionLocked(this.mDisplayContent);
            if (this.mSupportActiveModeSwitch) {
                this.mDisplayManagerInternal.updateDisplaySize(0, this.mMiuiDisplayWidth, this.mMiuiDisplayHeight);
            }
        }
    }

    private int calcDensityAndUpdateForceDensityIfNeed(int desireWidth) {
        int i;
        int density = getForcedDensity();
        if (!isDensityForced()) {
            return Math.round(((this.mPhysicalDensity * desireWidth) * 1.0f) / this.mPhysicalWidth);
        }
        if (this.mWmService.mSystemBooted && (i = this.mMiuiDisplayWidth) != 0) {
            int density2 = Math.round(((desireWidth * density) * 1.0f) / i);
            this.mDisplayContent.mBaseDisplayDensity = density2;
            Settings.Secure.putStringForUser(this.mWmService.mContext.getContentResolver(), "display_density_forced", density2 + "", UserHandle.getCallingUserId());
            return density2;
        }
        return density;
    }

    private boolean isDensityForced() {
        return getForcedDensity() != 0;
    }

    private void updateScreenResolutionLocked(DisplayContent dc) {
        boolean z = false;
        dc.mIsSizeForced = (dc.mInitialDisplayWidth == this.mMiuiDisplayWidth && dc.mInitialDisplayHeight == this.mMiuiDisplayHeight) ? false : true;
        if (dc.mInitialDisplayDensity != this.mMiuiDisplayDensity) {
            z = true;
        }
        dc.mIsDensityForced = z;
        dc.updateBaseDisplayMetrics(this.mMiuiDisplayWidth, this.mMiuiDisplayHeight, this.mMiuiDisplayDensity, dc.mBaseDisplayPhysicalXDpi, dc.mBaseDisplayPhysicalYDpi);
        dc.reconfigureDisplayLocked();
    }

    public void finishSwitchResolution() {
        synchronized (this.mGlobalLock) {
            this.mDisplayContent.setRoundedCornerOverlaysCanScreenShot(false);
            if (this.mPendingSwitchResolution) {
                Slog.i(TAG, "finished switching resolution");
                this.mPendingSwitchResolution = false;
            }
        }
    }

    public int getForcedDensity() {
        String densityString = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "display_density_forced", UserHandle.getCallingUserId());
        if (!TextUtils.isEmpty(densityString)) {
            return Integer.valueOf(densityString).intValue();
        }
        return 0;
    }

    public void initializeMiuiResolutionLocked() {
        if (!this.mSupportSwitchResolutionFeature) {
            return;
        }
        this.mDisplayContent = this.mWmService.mRoot.getDisplayContent(0);
        int[] screenResolution = getUserSetResolution();
        int screenWidth = screenResolution != null ? screenResolution[0] : this.mPhysicalWidth;
        int screenHeight = screenResolution != null ? screenResolution[1] : this.mPhysicalHeight;
        switchResolutionInternal(screenWidth, screenHeight);
        this.mPendingSwitchResolution = false;
    }

    private boolean isSupportSwitchResoluton() {
        boolean supportSwitchResolution = false;
        int[] mScreenResolutionsSupported = FeatureParser.getIntArray("screen_resolution_supported");
        if (mScreenResolutionsSupported != null && mScreenResolutionsSupported.length > 1) {
            supportSwitchResolution = true;
        }
        Slog.i(TAG, "isSupportSwitchResoluton:" + supportSwitchResolution);
        return supportSwitchResolution;
    }

    private void checkDDICSupportAndInitPhysicalSize() {
        DisplayInfo defaultDisplayInfo = this.mDisplayManagerInternal.getDisplayInfo(0);
        Display.Mode[] modes = defaultDisplayInfo.supportedModes;
        for (Display.Mode mode : modes) {
            if (this.mPhysicalHeight != 0 && mode.getPhysicalHeight() != this.mPhysicalHeight) {
                this.mSupportActiveModeSwitch = true;
            }
            if (mode.getPhysicalHeight() > this.mPhysicalHeight) {
                this.mPhysicalWidth = mode.getPhysicalWidth();
                this.mPhysicalHeight = mode.getPhysicalHeight();
            }
        }
        this.mPhysicalDensity = SystemProperties.getInt(BUILD_DENSITY_PROP, 560);
        Slog.i(TAG, "init resolution mSupportActiveModeSwitch:" + this.mSupportActiveModeSwitch + " mPhysicalWidth:" + this.mPhysicalWidth + " mPhysicalHeight:" + this.mPhysicalHeight + " mPhysicalDensity:" + this.mPhysicalDensity);
    }

    public int[] getUserSetResolution() {
        int[] screenSizeInfo = new int[3];
        String miuiResolution = SystemProperties.get("persist.sys.miui_resolution", (String) null);
        if (!TextUtils.isEmpty(miuiResolution)) {
            try {
                screenSizeInfo[0] = Integer.parseInt(miuiResolution.split(",")[0]);
                screenSizeInfo[1] = Integer.parseInt(miuiResolution.split(",")[1]);
                screenSizeInfo[2] = Integer.parseInt(miuiResolution.split(",")[2]);
                return screenSizeInfo;
            } catch (NumberFormatException e) {
                Slog.e(TAG, "getResolutionFromProperty exception:" + e.toString());
                return null;
            }
        }
        return null;
    }

    public int getDefaultDensity() {
        DisplayContent displayContent = this.mDisplayContent;
        if (displayContent == null) {
            return -1;
        }
        if (this.mSupportSwitchResolutionFeature) {
            int i = this.mPhysicalWidth;
            int i2 = this.mMiuiDisplayWidth;
            if (i * i2 != 0) {
                return Math.round(((this.mPhysicalDensity * i2) * 1.0f) / i);
            }
        }
        return displayContent.mInitialDisplayDensity;
    }

    public boolean isPendingSwitchResolution() {
        return this.mPendingSwitchResolution;
    }

    public boolean isSupportSetActiveModeSwitchResolution() {
        return this.mSupportActiveModeSwitch;
    }

    static void checkBoostPriorityForLockTime(long startBoostPriorityTime) {
        long endBoostPriorityTime = SystemClock.uptimeMillis();
        if (endBoostPriorityTime - startBoostPriorityTime > ActivityManagerServiceImpl.BOOST_DURATION) {
            Slog.w(TAG, "Slow operation: holding wms lock in " + Debug.getCallers(2) + " " + (endBoostPriorityTime - startBoostPriorityTime) + "ms");
        }
    }

    public static List<String> getProjectionBlackList() {
        if (mProjectionBlackList.size() == 0) {
            mProjectionBlackList.add("StatusBar");
            mProjectionBlackList.add("Splash Screen com.android.incallui");
            mProjectionBlackList.add("com.android.incallui/com.android.incallui.InCallActivity");
            mProjectionBlackList.add("FloatAssistantView");
            mProjectionBlackList.add("MiuiFreeformBorderView");
            mProjectionBlackList.add("SnapshotStartingWindow for");
            mProjectionBlackList.add("ScreenshotThumbnail");
            mProjectionBlackList.add("com.milink.ui.activity.ScreeningConsoleWindow");
            mProjectionBlackList.add("FloatNotificationPanel");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.AVActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity");
            mProjectionBlackList.add("com.tencent.mobileqq/.FloatingWindow");
            mProjectionBlackList.add("Splash Screen com.tencent.mm");
            mProjectionBlackList.add("com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity");
            mProjectionBlackList.add("com.tencent.mm/.FloatingWindow");
            mProjectionBlackList.add("com.whatsapp/com.whatsapp.voipcalling.VoipActivityV2");
            mProjectionBlackList.add("com.google.android.dialer/com.android.incallui.InCallActivity");
            mProjectionBlackList.add("com.google.android.dialer/.FloatingWindow");
            mProjectionBlackList.add("com.miui.yellowpage/com.miui.yellowpage.activity.MarkNumberActivity");
            mProjectionBlackList.add("com.miui.securitycenter/.FloatingWindow");
            mProjectionBlackList.add("com.milink.service.ui.PrivateWindow");
            mProjectionBlackList.add("com.milink.ui.activity.NFCLoadingActivity");
            mProjectionBlackList.add("Freeform-OverLayView");
            mProjectionBlackList.add("Freeform-HotSpotView");
            mProjectionBlackList.add("Freeform-TipView");
        }
        return mProjectionBlackList;
    }

    public static void setProjectionBlackList(List<String> blacklist) {
        mProjectionBlackList = blacklist;
    }

    public static boolean getLastFrame(String name) {
        if (name.contains("Splash Screen com.android.incallui") || name.contains("com.android.incallui/com.android.incallui.InCallActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity") || name.contains("Splash Screen com.tencent.mm") || name.contains("com.tencent.mm/com.tencent.mm.plugin.voip.ui.VideoActivity") || name.contains("com.google.android.dialer/com.android.incallui.InCallActivity") || name.contains("com.whatsapp/com.whatsapp.voipcalling.VoipActivityV2")) {
            return true;
        }
        return false;
    }

    public static void setAlertWindowTitle(WindowManager.LayoutParams attrs) {
        if (!WindowManager.LayoutParams.isSystemAlertWindowType(attrs.type)) {
            return;
        }
        if (QQ.equals(attrs.packageName)) {
            attrs.setTitle(QQ_FLOATING);
        }
        if (MM.equals(attrs.packageName)) {
            attrs.setTitle(MM_FLOATING);
        }
        if (GOOGLE.equals(attrs.packageName)) {
            attrs.setTitle(GOOGLE_FLOATING);
        }
        if (SECURITY.equals(attrs.packageName)) {
            attrs.setTitle(SECURITY_FLOATING);
        }
    }

    public void registerMiuiOptimizationObserver(Context context) {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.WindowManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                WindowManagerServiceImpl.this.IS_CTS_MODE = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
    }

    public boolean isCtsModeEnabled() {
        return this.IS_CTS_MODE;
    }

    int getBestDensityLocked(boolean miuiOptimization, DisplayContent displayContent) {
        if (miuiOptimization) {
            return this.mMiuiDisplayDensity;
        }
        return Math.round(((this.mMiuiDisplayDensity * 1.0f) * displayContent.mInitialDisplayDensity) / MiuiDisplayMetrics.DENSITY_DEVICE);
    }

    public boolean isSplitMode() {
        return this.mSplitMode;
    }

    public void setSplittable(boolean splittable) {
        this.mSplitMode = splittable;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void enableWmsDebugConfig(String config, boolean enable) {
        char c;
        Slog.d(TAG, "enableWMSDebugConfig, config=:" + config + ", enable=:" + enable);
        switch (config.hashCode()) {
            case -2091566946:
                if (config.equals("DEBUG_VISIBILITY")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1682081203:
                if (config.equals("DEBUG_WALLPAPER_LIGHT")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case -1610158464:
                if (config.equals("SHOW_LIGHT_TRANSACTIONS")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case -1411240286:
                if (config.equals("DEBUG_WINDOW_TRACE")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -1326138211:
                if (config.equals("DEBUG_ANIM")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -1326045248:
                if (config.equals("DEBUG_DRAG")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -1149250858:
                if (config.equals("DEBUG_WALLPAPER")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -663252277:
                if (config.equals("DEBUG_TASK_POSITIONING")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case -154379534:
                if (config.equals("DEBUG_SCREENSHOT")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 64921139:
                if (config.equals("DEBUG")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 654509718:
                if (config.equals("DEBUG_DISPLAY")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 663952660:
                if (config.equals("SHOW_VERBOSE_TRANSACTIONS")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 833576886:
                if (config.equals("DEBUG_ROOT_TASK")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 937928650:
                if (config.equals("DEBUG_CONFIGURATION")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1201042717:
                if (config.equals("DEBUG_TASK_MOVEMENT")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1269506447:
                if (config.equals("DEBUG_LAYOUT_REPEATS")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case 1278188518:
                if (config.equals("DEBUG_STARTING_WINDOW_VERBOSE")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 1298095333:
                if (config.equals("SHOW_STACK_CRAWLS")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case 1477990771:
                if (config.equals("DEBUG_WINDOW_CROP")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case 1489852622:
                if (config.equals("DEBUG_LAYERS")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1489862326:
                if (config.equals("DEBUG_LAYOUT")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1846783646:
                if (config.equals("DEBUG_INPUT")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1853284313:
                if (config.equals("DEBUG_POWER")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case 2026833314:
                if (config.equals("DEBUG_INPUT_METHOD")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 2105417809:
                if (config.equals("DEBUG_UNKNOWN_APP_VISIBILITY")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                WindowManagerDebugConfig.DEBUG = enable;
                return;
            case 1:
                WindowManagerDebugConfig.DEBUG_ANIM = enable;
                return;
            case 2:
                WindowManagerDebugConfig.DEBUG_LAYOUT = enable;
                return;
            case 3:
                WindowManagerDebugConfig.DEBUG_LAYERS = enable;
                return;
            case 4:
                WindowManagerDebugConfig.DEBUG_INPUT = enable;
                return;
            case 5:
                WindowManagerDebugConfig.DEBUG_INPUT_METHOD = enable;
                return;
            case 6:
                WindowManagerDebugConfig.DEBUG_VISIBILITY = enable;
                return;
            case 7:
                WindowManagerDebugConfig.DEBUG_CONFIGURATION = enable;
                return;
            case '\b':
                WindowManagerDebugConfig.DEBUG_STARTING_WINDOW_VERBOSE = enable;
                return;
            case '\t':
                WindowManagerDebugConfig.DEBUG_WALLPAPER = enable;
                return;
            case '\n':
                WindowManagerDebugConfig.DEBUG_WALLPAPER_LIGHT = enable;
                return;
            case 11:
                WindowManagerDebugConfig.DEBUG_DRAG = enable;
                return;
            case '\f':
                WindowManagerDebugConfig.DEBUG_SCREENSHOT = enable;
                return;
            case '\r':
                WindowManagerDebugConfig.DEBUG_LAYOUT_REPEATS = enable;
                return;
            case 14:
                WindowManagerDebugConfig.DEBUG_WINDOW_TRACE = enable;
                return;
            case 15:
                WindowManagerDebugConfig.DEBUG_TASK_MOVEMENT = enable;
                return;
            case 16:
                WindowManagerDebugConfig.DEBUG_TASK_POSITIONING = enable;
                return;
            case 17:
                WindowManagerDebugConfig.DEBUG_ROOT_TASK = enable;
                return;
            case 18:
                WindowManagerDebugConfig.DEBUG_DISPLAY = enable;
                return;
            case 19:
                WindowManagerDebugConfig.DEBUG_POWER = enable;
                return;
            case 20:
                WindowManagerDebugConfig.SHOW_VERBOSE_TRANSACTIONS = enable;
                return;
            case 21:
                WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS = enable;
                return;
            case 22:
                WindowManagerDebugConfig.SHOW_STACK_CRAWLS = enable;
                return;
            case 23:
                WindowManagerDebugConfig.DEBUG_WINDOW_CROP = enable;
                return;
            case 24:
                WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY = enable;
                return;
            default:
                return;
        }
    }

    public void setCloudDimControllerList(String DimCloudConfig) {
        String[] DimConfigArray = DimCloudConfig.split(";");
        this.mDimUserTimeoutMap.clear();
        this.mDimNeedAssistMap.clear();
        for (String str : DimConfigArray) {
            String[] DimConfigTotal = str.split(",");
            this.mDimUserTimeoutMap.put(DimConfigTotal[0], Integer.valueOf(Integer.parseInt(DimConfigTotal[1])));
            this.mDimNeedAssistMap.put(DimConfigTotal[0], Boolean.valueOf(Boolean.parseBoolean(DimConfigTotal[2])));
        }
    }

    public int getUserActivityTime(String name) {
        return this.mDimUserTimeoutMap.get(name).intValue();
    }

    public boolean isAdjustScreenOff(CharSequence tag) {
        if (!SUPPPORT_CLOUD_DIM) {
            return false;
        }
        String targetWindowName = tag.toString();
        return !this.mDimUserTimeoutMap.isEmpty() && this.mDimUserTimeoutMap.containsKey(targetWindowName);
    }

    public boolean isAssistResson(CharSequence tag) {
        String targetWindowName = tag.toString();
        if (!this.mDimNeedAssistMap.isEmpty() && this.mDimNeedAssistMap.containsKey(targetWindowName) && this.mDimNeedAssistMap.get(targetWindowName).booleanValue()) {
            return true;
        }
        return false;
    }

    public boolean isCameraOpen() {
        return this.mOpeningCameraID.size() > 0;
    }

    public void linkWallpaperWindowTokenDeathMonitor(IBinder binder, int displayId) {
        try {
            binder.linkToDeath(new WallpaperDeathMonitor(binder, displayId), 0);
        } catch (RemoteException e) {
        }
    }

    public void addSecureChangedListener(IWindowSecureChangeListener listener) {
        synchronized (this.mGlobalLock) {
            if (listener != null) {
                if (!this.mSecureChangeListeners.contains(listener)) {
                    this.mSecureChangeListeners.add(listener);
                }
            }
        }
    }

    public void removeSecureChangedListener(IWindowSecureChangeListener listener) {
        synchronized (this.mGlobalLock) {
            if (listener != null) {
                if (this.mSecureChangeListeners.contains(listener)) {
                    this.mSecureChangeListeners.remove(listener);
                }
            }
        }
    }

    public void onSecureChangedListener(WindowState windowState, boolean hasSecure) {
        synchronized (this.mGlobalLock) {
            if (this.mSecureChangeListeners.isEmpty()) {
                return;
            }
            Iterator<IWindowSecureChangeListener> it = this.mSecureChangeListeners.iterator();
            while (it.hasNext()) {
                IWindowSecureChangeListener listener = it.next();
                try {
                    listener.onSecureChangeCallback(windowState.getWindowTag().toString(), hasSecure);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void registerSettingsObserver(Context context, WindowManagerService.SettingsObserver settingsObserver) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(this.mDarkModeEnable, false, settingsObserver, -1);
        resolver.registerContentObserver(this.mDarkModeContrastEnable, false, settingsObserver, -1);
    }

    public boolean onSettingsObserverChange(boolean selfChange, Uri uri) {
        if (this.mDarkModeEnable.equals(uri) || this.mDarkModeContrastEnable.equals(uri) || this.mContrastAlphaUri.equals(uri)) {
            Slog.d(TAG, "updateContrast : " + uri);
            updateContrastAlpha(!this.isFpClientOn);
            return true;
        }
        return false;
    }

    public void updateContrastAlpha(final boolean darkmode) {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl.5
            @Override // java.lang.Runnable
            public void run() {
                float alpha = Settings.System.getFloat(WindowManagerServiceImpl.this.mContext.getContentResolver(), "contrast_alpha", MiuiFreeformPinManagerService.EDGE_AREA);
                if (alpha >= 0.5f) {
                    alpha = MiuiFreeformPinManagerService.EDGE_AREA;
                }
                boolean isContrastEnabled = WindowManagerServiceImpl.this.isDarkModeContrastEnable();
                Slog.i(WindowManagerServiceImpl.TAG, "updateContrastOverlay, darkmode: " + darkmode + " isContrastEnabled: " + isContrastEnabled + " alpha: " + alpha);
                synchronized (WindowManagerServiceImpl.this.mWmService.mWindowMap) {
                    WindowManagerServiceImpl.this.mWmService.openSurfaceTransaction();
                    if (darkmode && isContrastEnabled) {
                        if (WindowManagerServiceImpl.this.mMiuiContrastOverlay == null) {
                            WindowManagerServiceImpl.this.mMiuiContrastOverlay = MiuiContrastOverlayStub.getInstance();
                            DisplayContent displayContent = WindowManagerServiceImpl.this.mWmService.getDefaultDisplayContentLocked();
                            WindowManagerServiceImpl.this.mMiuiContrastOverlay.init(displayContent, displayContent.mRealDisplayMetrics, WindowManagerServiceImpl.this.mContext);
                        }
                        WindowManagerServiceImpl.this.mMiuiContrastOverlay.showContrastOverlay(alpha);
                    } else {
                        if (WindowManagerServiceImpl.this.mMiuiContrastOverlay != null) {
                            Slog.i(WindowManagerServiceImpl.TAG, " hideContrastOverlay ");
                            WindowManagerServiceImpl.this.mMiuiContrastOverlay.hideContrastOverlay();
                        }
                        WindowManagerServiceImpl.this.mMiuiContrastOverlay = null;
                    }
                    WindowManagerServiceImpl.this.mWmService.closeSurfaceTransaction("MiuiContrastOverlay");
                }
            }
        });
    }

    public boolean isDarkModeContrastEnable() {
        boolean z = true;
        if ((this.mContext.getResources().getConfiguration().uiMode & 48) != 32 || !MiuiSettings.System.getBoolean(this.mContext.getContentResolver(), "dark_mode_contrast_enable", true)) {
            z = false;
        }
        boolean isContrastEnable = z;
        return isContrastEnable;
    }

    public void registerUiModeAnimFinishedCallback(IWindowAnimationFinishedCallback callback) {
        this.mUiModeAnimFinishedCallback = callback;
    }

    public void onAnimationFinished() {
        try {
            IWindowAnimationFinishedCallback iWindowAnimationFinishedCallback = this.mUiModeAnimFinishedCallback;
            if (iWindowAnimationFinishedCallback != null) {
                iWindowAnimationFinishedCallback.onWindowAnimFinished();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "Call mUiModeAnimFinishedCallback.onWindowAnimFinished error " + e);
        }
    }

    public void positionContrastSurface(int defaultDw, int defaultDh) {
        MiuiContrastOverlayStub miuiContrastOverlayStub = this.mMiuiContrastOverlay;
        if (miuiContrastOverlayStub != null) {
            miuiContrastOverlayStub.positionSurface(defaultDw, defaultDh);
        }
    }

    public void updateScreenShareProjectFlag() {
        this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerServiceImpl.this.m1901x45bd62ee();
            }
        });
    }

    /* renamed from: lambda$updateScreenShareProjectFlag$1$com-android-server-wm-WindowManagerServiceImpl */
    public /* synthetic */ void m1901x45bd62ee() {
        synchronized (this.mGlobalLock) {
            this.mWmService.mRoot.forAllWindows(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    WindowManagerServiceImpl.this.m1900xbf2c10f((WindowState) obj);
                }
            }, false);
        }
    }

    /* renamed from: lambda$updateScreenShareProjectFlag$0$com-android-server-wm-WindowManagerServiceImpl */
    public /* synthetic */ void m1900xbf2c10f(WindowState w) {
        SurfaceControl surfaceControl;
        if (w != null && w.mWinAnimator != null && w.mWinAnimator.mSurfaceController != null && (surfaceControl = w.mWinAnimator.mSurfaceController.mSurfaceControl) != null) {
            int flags = getScreenShareProjectAndPrivateCastFlag(w.mWinAnimator);
            surfaceControl.setScreenProjection(flags);
        }
    }

    public int getScreenShareProjectAndPrivateCastFlag(WindowStateAnimator winAnimator) {
        ArrayList<String> list;
        int applyFlag = 0;
        IMiuiScreenProjectionStub imp = IMiuiScreenProjectionStub.getInstance();
        boolean isScreenShareProtection = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "screen_share_protection_on", 0, -2) == 1;
        int screenProjectionOnOrOff = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), imp.getMiuiInScreeningSettingsKey(), 0, -2);
        int screenProjectionPrivacy = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), imp.getMiuiPrivacyOnSettingsKey(), 0, -2);
        if (winAnimator != null && winAnimator.mWin != null && winAnimator.mWin.getAttrs() != null && winAnimator.mWin.getAttrs().getTitle() != null) {
            String name = winAnimator.mWin.getAttrs().getTitle().toString();
            if (screenProjectionOnOrOff > 0 && screenProjectionPrivacy > 0 && (list = imp.getProjectionBlackList()) != null) {
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    String blackTitle = it.next();
                    if (name.contains(blackTitle)) {
                        applyFlag |= imp.getExtraScreenProjectFlag();
                    } else if (winAnimator.mWin.mIsImWindow && winAnimator.mWin.getName() != null && winAnimator.mWin.getName().contains("PopupWindow") && winAnimator.mWin.mViewVisibility == 0) {
                        applyFlag |= imp.getExtraScreenProjectFlag();
                    }
                }
            }
            int mode = 0;
            String packageName = null;
            if (winAnimator.mWin.mActivityRecord != null && winAnimator.mWin.mActivityRecord.info != null) {
                int uid = winAnimator.mWin.mActivityRecord.info.applicationInfo.uid;
                packageName = winAnimator.mWin.mActivityRecord.info.applicationInfo.packageName;
                mode = this.mAppOps.checkOpNoThrow(10041, uid, packageName);
            }
            if (isScreenShareProtection) {
                if (mode == 1 && packageName != null) {
                    applyFlag |= 1;
                }
                ArrayList<String> screenShareProjectBlacklist = imp.getScreenShareProjectBlackList();
                if (screenShareProjectBlacklist == null) {
                    return applyFlag;
                }
                Iterator<String> it2 = screenShareProjectBlacklist.iterator();
                while (it2.hasNext()) {
                    String shareName = it2.next();
                    if (name.contains(shareName)) {
                        applyFlag |= 2;
                    }
                }
            }
        }
        return applyFlag;
    }

    public void notifyTouchFromNative(boolean isTouched) {
        PowerKeeperPolicy powerKeeperPolicy = this.mPowerKeeperPolicy;
        if (powerKeeperPolicy == null) {
            return;
        }
        powerKeeperPolicy.notifyTouchStatus(isTouched);
    }

    public void notifyFloatWindowScene(String packageName, int windowType, boolean status) {
        PowerKeeperPolicy powerKeeperPolicy = this.mPowerKeeperPolicy;
        if (powerKeeperPolicy == null) {
            return;
        }
        powerKeeperPolicy.notifyFloatWindowScene(packageName, windowType, status);
    }

    public void notifySystemBrightnessChange() {
        this.mWmService.mH.post(new Runnable() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                WindowManagerServiceImpl.this.m1899x99e5918e();
            }
        });
    }

    /* renamed from: lambda$notifySystemBrightnessChange$3$com-android-server-wm-WindowManagerServiceImpl */
    public /* synthetic */ void m1899x99e5918e() {
        ArrayList<Task> visibleTasks;
        TaskDisplayArea defaultTaskDisplayArea = this.mWmService.mRoot.getDefaultTaskDisplayArea();
        synchronized (this.mGlobalLock) {
            visibleTasks = defaultTaskDisplayArea.getVisibleTasks();
        }
        visibleTasks.forEach(new Consumer() { // from class: com.android.server.wm.WindowManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                WindowManagerServiceImpl.this.m1898x601aefaf((Task) obj);
            }
        });
    }

    /* renamed from: lambda$notifySystemBrightnessChange$2$com-android-server-wm-WindowManagerServiceImpl */
    public /* synthetic */ void m1898x601aefaf(Task task) {
        notifySystemBrightnessChange(task.getTopVisibleAppMainWindow());
    }

    private void notifySystemBrightnessChange(WindowState w) {
        if (w == null || w.mAttrs.screenBrightness == -1.0f || this.mTaskIdScreenBrightnessOverrides.contains(w.getTask().mTaskId)) {
            return;
        }
        this.mTaskIdScreenBrightnessOverrides.put(w.getTask().mTaskId, Float.valueOf(w.mAttrs.screenBrightness));
        this.mWmService.mPowerManagerInternal.setScreenBrightnessOverrideFromWindowManager(Float.NaN);
    }

    public boolean shouldApplyOverrideBrightness(WindowState w) {
        Task task = w.getTask();
        if (task == null) {
            return true;
        }
        int index = this.mTaskIdScreenBrightnessOverrides.indexOfKey(task.mTaskId);
        if (index < 0) {
            return true;
        }
        if (this.mTaskIdScreenBrightnessOverrides.get(task.mTaskId).floatValue() != w.mAttrs.screenBrightness && !this.mBlackList.contains(w.getOwningPackage())) {
            Slog.i(TAG, "shouldApplyOverrideBrightness: mTaskIdScreenBrightnessOverrides=" + this.mTaskIdScreenBrightnessOverrides + " taskId : " + task.mTaskId);
            this.mTaskIdScreenBrightnessOverrides.delete(task.mTaskId);
            return true;
        }
        return false;
    }

    public void clearOverrideBrightnessRecord(int taskId) {
        int index = this.mTaskIdScreenBrightnessOverrides.indexOfKey(taskId);
        if (index >= 0) {
            Slog.i(TAG, "onTaskRemoved: mTaskIdScreenBrightnessOverrides : " + this.mTaskIdScreenBrightnessOverrides + " taskId : " + taskId);
            this.mTaskIdScreenBrightnessOverrides.delete(taskId);
        }
    }

    public void updateSurfaceParentIfNeed(WindowState ws) {
        WindowManagerService windowManagerService;
        if (ws == null || (windowManagerService = this.mWmService) == null || windowManagerService.mAtmService == null) {
            return;
        }
        Task topRootTask1 = this.mWmService.mAtmService.getTopDisplayFocusedRootTask();
        Task topRootTask2 = this.mWmService.mRoot.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
        Task pairRootTask = null;
        if (ActivityTaskManagerServiceImpl.getInstance().isInSystemSplitScreen(topRootTask1)) {
            pairRootTask = topRootTask1;
        } else if (ActivityTaskManagerServiceImpl.getInstance().isInSystemSplitScreen(topRootTask2)) {
            pairRootTask = topRootTask2;
        }
        if (pairRootTask != null && CLIENT_DIMMER_NAME.equals(ws.getWindowTag()) && ws.getSurfaceControl() != null && ws.getSurfaceControl().isValid() && pairRootTask.getSurfaceControl() != null && pairRootTask.getSurfaceControl().isValid()) {
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setRelativeLayer(ws.getSurfaceControl(), pairRootTask.mSurfaceControl, Integer.MAX_VALUE);
            t.apply();
        }
    }

    public void setRunningRecentsAnimation(boolean running) {
        this.mRunningRecentsAnimation = running;
    }

    public void showPinnedTaskIfNeeded(Task task) {
        if (!this.mRunningRecentsAnimation && task.isRootTask() && task.isVisible()) {
            task.getPendingTransaction().show(task.mSurfaceControl);
            Slog.d(TAG, "show pinned task surface: " + task);
        }
    }

    public float getCompatScale(String packageName, int uid) {
        return this.mWmService.mAtmService.mCompatModePackages.getCompatScale(packageName, uid);
    }
}
