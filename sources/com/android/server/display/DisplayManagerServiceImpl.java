package com.android.server.display;

import android.content.Context;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.LocalDisplayAdapter;
import com.android.server.display.VirtualDisplayAdapter;
import com.android.server.wm.MiuiRefreshRatePolicy;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import miui.security.SecurityManagerInternal;
/* loaded from: classes.dex */
public class DisplayManagerServiceImpl implements DisplayManagerServiceStub {
    private static boolean DEBUG = false;
    private static final int FLAG_INCREASE_SCREEN_BRIGHTNESS = 0;
    private static final int MAX_SIZE_FOR_PER_THIRD_APP = 10;
    static final int MSG_RESET_SHORT_MODEL = 255;
    private static final String TAG = "DisplayManagerServiceImpl";
    private boolean mBootCompleted;
    private Context mContext;
    private DisplayDevice mDefaultDisplayDevice;
    private IBinder mDefaultDisplayToken;
    private LocalDisplayAdapter.LocalDisplayDevice mDefaultLocalDisplayDevice;
    private LogicalDisplay mDefaultLogicalDisplay;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private Handler mHandler;
    private Object mLock;
    private LogicalDisplayMapper mLogicalDisplayMapper;
    private MiuiFoldPolicy mMiuiFoldPolicy;
    private SecurityManagerInternal mSecurityManager;
    private boolean mIsResetRate = false;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks = new HashMap<>();
    private List<String> mResolutionSwitchProcessProtectList = new ArrayList();
    private List<String> mResolutionSwitchProcessBlackList = new ArrayList();
    private final SparseArray<DisplayModeDirector.Vote> mVotes = new SparseArray<>();
    private List<DisplayDevice> mDisplayDevices = new ArrayList();
    private int[] mScreenEffectDisplayIndex = {0};

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayManagerServiceImpl> {

        /* compiled from: DisplayManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayManagerServiceImpl INSTANCE = new DisplayManagerServiceImpl();
        }

        public DisplayManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayManagerServiceImpl provideNewInstance() {
            return new DisplayManagerServiceImpl();
        }
    }

    /* loaded from: classes.dex */
    public enum MultiDisplayType {
        DISPLAY_TYPE_SINGLE,
        DISPLAY_TYPE_MULTI_NORMAL,
        DISPLAY_TYPE_MULTI_FOLDER;

        public boolean isFolderDisplay() {
            return this == DISPLAY_TYPE_MULTI_FOLDER;
        }

        public boolean isMultiDisplayNormal() {
            return this == DISPLAY_TYPE_MULTI_NORMAL;
        }
    }

    MultiDisplayType MultiDisplayTypeFormInt(int type) {
        try {
            return MultiDisplayType.values()[type];
        } catch (IndexOutOfBoundsException e) {
            Slog.d(TAG, "Invalid Request type, return default");
            return MultiDisplayType.DISPLAY_TYPE_SINGLE;
        }
    }

    public void init(Object lock, Context context, Looper looper, LogicalDisplayMapper logicalDisplayMapper) {
        this.mLock = lock;
        this.mContext = context;
        this.mMiuiFoldPolicy = new MiuiFoldPolicy(this.mContext);
        this.mHandler = new DisplayManagerStubHandler(looper);
        this.mLogicalDisplayMapper = logicalDisplayMapper;
    }

    public static DisplayManagerServiceImpl getInstance() {
        return (DisplayManagerServiceImpl) MiuiStubUtil.getImpl(DisplayManagerServiceStub.class);
    }

    public void setUpDisplayPowerControllerImpl(DisplayPowerControllerImpl impl) {
        this.mDisplayPowerControllerImpl = impl;
    }

    public void updateDeviceDisplayChanged(DisplayDevice device, int event) {
        DisplayDeviceInfo info;
        if (device instanceof LocalDisplayAdapter.LocalDisplayDevice) {
            synchronized (this.mLock) {
                switch (event) {
                    case 1:
                        if (!this.mDisplayDevices.contains(device)) {
                            this.mDisplayDevices.add(device);
                            updateScreenEffectDisplayIndexLocked();
                            break;
                        }
                        break;
                    case 3:
                        if (this.mDisplayDevices.contains(device)) {
                            this.mDisplayDevices.remove(device);
                            updateScreenEffectDisplayIndexLocked();
                            break;
                        }
                        break;
                }
            }
        } else if ((device instanceof VirtualDisplayAdapter.VirtualDisplayDevice) && (info = device.getDisplayDeviceInfoLocked()) != null) {
            if (this.mSecurityManager == null) {
                this.mSecurityManager = (SecurityManagerInternal) LocalServices.getService(SecurityManagerInternal.class);
            }
            this.mSecurityManager.onDisplayDeviceEvent(info.ownerPackageName, info.name, device.getDisplayTokenLocked(), event);
            WindowManagerServiceStub.get().updateScreenShareProjectFlag();
        }
    }

    private int[] updateScreenEffectDisplayIndexLocked() {
        int[] iArr;
        synchronized (this.mLock) {
            List<Integer> displayIndex = new ArrayList<>();
            for (int i = 0; i < this.mDisplayDevices.size(); i++) {
                displayIndex.add(Integer.valueOf(i));
            }
            int i2 = displayIndex.size();
            this.mScreenEffectDisplayIndex = new int[i2];
            for (int i3 = 0; i3 < displayIndex.size(); i3++) {
                this.mScreenEffectDisplayIndex[i3] = displayIndex.get(i3).intValue();
            }
            iArr = this.mScreenEffectDisplayIndex;
        }
        return iArr;
    }

    public int[] getScreenEffectAvailableDisplayInternal() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mScreenEffectDisplayIndex;
        }
        return iArr;
    }

    private int getScreenEffectDisplayIndexInternal(long physicalDisplayId) {
        IBinder displayToken = SurfaceControl.getPhysicalDisplayToken(physicalDisplayId);
        synchronized (this.mLock) {
            for (int i = 0; i < this.mDisplayDevices.size(); i++) {
                if (displayToken == this.mDisplayDevices.get(i).getDisplayTokenLocked()) {
                    return i;
                }
            }
            return 0;
        }
    }

    boolean getScreenEffectAvailableDisplay(Parcel data, Parcel reply) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            int[] result = getScreenEffectAvailableDisplayInternal();
            reply.writeInt(result.length);
            reply.writeIntArray(result);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    boolean getScreenEffectDisplayIndex(Parcel data, Parcel reply) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            int result = getScreenEffectDisplayIndexInternal(data.readLong());
            reply.writeInt(result);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public boolean onTransact(Handler displayControllerHandler, int code, Parcel data, Parcel reply, int flags) {
        if (code == 16777214) {
            return resetAutoBrightnessShortModel(displayControllerHandler, data);
        }
        if (code == 16777213) {
            return setBrightnessRate(data);
        }
        if (code == 16777212) {
            return getScreenEffectAvailableDisplay(data, reply);
        }
        if (code == 16777211) {
            return getScreenEffectDisplayIndex(data, reply);
        }
        if (code == 16777210) {
            return setVideoInformation(data);
        }
        if (code == 16777209) {
            return handleGalleryHdrRequest(data);
        }
        if (code == 16777208) {
            appRequestChangeSceneRefreshRate(data);
            return false;
        }
        return false;
    }

    private boolean appRequestChangeSceneRefreshRate(Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        int callingUid = Binder.getCallingUid();
        String targetPkgName = data.readString();
        int maxRefreshRate = data.readInt();
        if (callingUid < 10000) {
            long token = Binder.clearCallingIdentity();
            try {
                if (maxRefreshRate == -1) {
                    MiuiRefreshRatePolicy.getInstance().removeRefreshRateRangeForPackage(targetPkgName);
                } else {
                    MiuiRefreshRatePolicy.getInstance().addSceneRefreshRateForPackage(targetPkgName, maxRefreshRate);
                }
                Binder.restoreCallingIdentity(token);
                return true;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(token);
                throw th;
            }
        }
        return false;
    }

    private boolean setVideoInformation(Parcel data) {
        Throwable th;
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.VIDEO_INFORMATION", "Permission required to set video information");
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        int pid = Binder.getCallingPid();
        boolean bulletChatStatus = data.readBoolean();
        float frameRate = data.readFloat();
        int width = data.readInt();
        int height = data.readInt();
        float compressionRatio = data.readFloat();
        IBinder token = data.readStrongBinder();
        Slog.d(TAG, "setVideoInformation bulletChatStatus:" + bulletChatStatus + ",pid:" + pid + ",token:" + token);
        if (pid > 0 && token != null) {
            long ident = Binder.clearCallingIdentity();
            try {
                try {
                    ScreenEffectService.sScreenEffectManager.setVideoInformation(pid, bulletChatStatus, frameRate, width, height, compressionRatio, token);
                    Binder.restoreCallingIdentity(ident);
                    return true;
                } catch (Throwable th2) {
                    th = th2;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        } else {
            return false;
        }
    }

    boolean resetAutoBrightnessShortModel(Handler displayControllerHandler, Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        resetAutoBrightnessShortModelInternal(displayControllerHandler);
        return true;
    }

    private void resetAutoBrightnessShortModelInternal(Handler displayControllerHandler) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            throw new SecurityException("Only system uid can reset Short Model!");
        }
        Slog.d(TAG, "reset AutoBrightness ShortModel");
        long token = Binder.clearCallingIdentity();
        if (displayControllerHandler != null) {
            try {
                displayControllerHandler.obtainMessage(MSG_RESET_SHORT_MODEL).sendToTarget();
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
    }

    private boolean setBrightnessRate(Parcel data) {
        int uid = Binder.getCallingUid();
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        long token = Binder.clearCallingIdentity();
        try {
            this.mIsResetRate = data.readBoolean();
            Slog.d(TAG, "setBrightnessRate, uid: " + uid + ", mIsResetRate: " + this.mIsResetRate);
            Binder.restoreCallingIdentity(token);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    public boolean getIsResetRate() {
        return this.mIsResetRate;
    }

    public Object getSyncRoot() {
        return this.mLock;
    }

    public void updateResolutionSwitchList(final List<String> resolutionSwitchProtectList, final List<String> resolutionSwitchBlackList) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DisplayManagerServiceImpl.this.m551x1fdb1b53(resolutionSwitchBlackList, resolutionSwitchProtectList);
            }
        });
    }

    /* renamed from: lambda$updateResolutionSwitchList$0$com-android-server-display-DisplayManagerServiceImpl */
    public /* synthetic */ void m551x1fdb1b53(List resolutionSwitchBlackList, List resolutionSwitchProtectList) {
        this.mResolutionSwitchProcessBlackList.clear();
        this.mResolutionSwitchProcessProtectList.clear();
        this.mResolutionSwitchProcessBlackList.addAll(resolutionSwitchBlackList);
        this.mResolutionSwitchProcessProtectList.addAll(resolutionSwitchProtectList);
    }

    public boolean isInResolutionSwitchProtectList(String processName) {
        return this.mResolutionSwitchProcessProtectList.contains(processName);
    }

    public boolean isInResolutionSwitchBlackList(String processName) {
        return this.mResolutionSwitchProcessBlackList.contains(processName);
    }

    /* loaded from: classes.dex */
    private class DisplayManagerStubHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DisplayManagerStubHandler(Looper looper) {
            super(looper, null, true);
            DisplayManagerServiceImpl.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    public SurfaceControl.DynamicDisplayInfo updateDefaultDisplaySupportMode() {
        synchronized (this.mLock) {
            updateDefaultDisplayLocked();
            if (this.mDefaultLogicalDisplay == null) {
                return null;
            }
            return SurfaceControl.getDynamicDisplayInfo(this.mDefaultDisplayToken);
        }
    }

    public void shouldUpdateDisplayModeSpecs(SurfaceControl.DesiredDisplayModeSpecs modeSpecs) {
        IBinder iBinder;
        LocalDisplayAdapter.LocalDisplayDevice localDisplayDevice = this.mDefaultLocalDisplayDevice;
        if (localDisplayDevice == null || (iBinder = this.mDefaultDisplayToken) == null) {
            return;
        }
        localDisplayDevice.setDesiredDisplayModeSpecsAsync(iBinder, modeSpecs);
        synchronized (this.mLock) {
            this.mDefaultLocalDisplayDevice.updateDesiredDisplayModeSpecsLocked(modeSpecs);
            DisplayModeDirector.DesiredDisplayModeSpecs desired = new DisplayModeDirector.DesiredDisplayModeSpecs(modeSpecs.defaultMode, modeSpecs.allowGroupSwitching, new DisplayManagerInternal.RefreshRateRange(modeSpecs.primaryRefreshRateMin, modeSpecs.primaryRefreshRateMax), new DisplayManagerInternal.RefreshRateRange(modeSpecs.appRequestRefreshRateMin, modeSpecs.appRequestRefreshRateMax));
            this.mDefaultLogicalDisplay.setDesiredDisplayModeSpecsLocked(desired);
            DisplayModeDirectorImpl.getInstance().onDesiredDisplayModeSpecsChanged(this.mDefaultLogicalDisplay.getDisplayIdLocked(), desired, this.mVotes);
        }
    }

    private void updateDefaultDisplayLocked() {
        LogicalDisplayMapper logicalDisplayMapper = this.mLogicalDisplayMapper;
        if (logicalDisplayMapper == null) {
            return;
        }
        LogicalDisplay displayLocked = logicalDisplayMapper.getDisplayLocked(0);
        this.mDefaultLogicalDisplay = displayLocked;
        if (displayLocked == null) {
            Slog.e(TAG, "get default display error");
        }
        LocalDisplayAdapter.LocalDisplayDevice primaryDisplayDeviceLocked = this.mDefaultLogicalDisplay.getPrimaryDisplayDeviceLocked();
        this.mDefaultDisplayDevice = primaryDisplayDeviceLocked;
        if (primaryDisplayDeviceLocked instanceof LocalDisplayAdapter.LocalDisplayDevice) {
            this.mDefaultLocalDisplayDevice = primaryDisplayDeviceLocked;
        }
        this.mDefaultDisplayToken = primaryDisplayDeviceLocked.getDisplayTokenLocked();
    }

    private boolean handleGalleryHdrRequest(Parcel data) {
        data.enforceInterface("android.view.android.hardware.display.IDisplayManager");
        IBinder token = data.readStrongBinder();
        boolean increaseScreenBrightness = data.readBoolean();
        requestGalleryHdrBoost(token, increaseScreenBrightness);
        return true;
    }

    private void requestGalleryHdrBoost(IBinder token, boolean enable) {
        long ident = Binder.clearCallingIdentity();
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        if (token != null) {
            try {
                synchronized (this.mLock) {
                    setDeathCallbackLocked(token, 0, enable);
                    DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
                    if (displayPowerControllerImpl != null) {
                        displayPowerControllerImpl.updateGalleryHdrState(enable);
                    }
                }
                Slog.w(TAG, "requestGalleryHdrBoost: callingUid: " + callingUid + ", callingPid: " + callingPid + ", enable: " + enable);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void setDeathCallbackLocked(IBinder token, int flag, boolean enable) {
        if (enable) {
            registerDeathCallbackLocked(token, flag);
        } else {
            unregisterDeathCallbackLocked(token);
        }
    }

    protected void registerDeathCallbackLocked(IBinder token, int flag) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.w(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token, flag));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        ClientDeathCallback deathCallback;
        if (token != null && (deathCallback = this.mClientDeathCallbacks.remove(token)) != null) {
            token.unlinkToDeath(deathCallback, 0);
        }
    }

    public boolean isGalleryHdrEnable() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.isGalleryHdrEnable();
        }
        return false;
    }

    public float getGalleryHdrBoostFactor(float sdrBacklight, float hdrBacklight) {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            return displayPowerControllerImpl.getGalleryHdrBoostFactor(sdrBacklight, hdrBacklight);
        }
        return 1.0f;
    }

    public void notifySystemBrightnessChange() {
        DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
        if (displayPowerControllerImpl != null) {
            displayPowerControllerImpl.notifySystemBrightnessChange();
        }
    }

    public void doDieLocked(int flag, IBinder token) {
        if (flag == 0) {
            unregisterDeathCallbackLocked(token);
            DisplayPowerControllerImpl displayPowerControllerImpl = this.mDisplayPowerControllerImpl;
            if (displayPowerControllerImpl != null) {
                displayPowerControllerImpl.updateGalleryHdrState(false);
            }
        }
    }

    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private int mFlag;
        private IBinder mToken;

        public ClientDeathCallback(DisplayManagerServiceImpl this$0, IBinder token) {
            this(token, 0);
        }

        public ClientDeathCallback(IBinder token, int flag) {
            DisplayManagerServiceImpl.this = this$0;
            this.mToken = token;
            this.mFlag = flag;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(DisplayManagerServiceImpl.TAG, "binderDied: flag: " + this.mFlag);
            synchronized (DisplayManagerServiceImpl.this.mLock) {
                DisplayManagerServiceImpl.this.doDieLocked(this.mFlag, this.mToken);
            }
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("DisplayManagerServiceImpl Configuration:");
        DEBUG = DisplayDebugConfig.DEBUG_DMS;
    }

    public void onBootCompleted() {
        this.mBootCompleted = true;
        if (this.mMiuiFoldPolicy != null && SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            this.mMiuiFoldPolicy.initMiuiFoldPolicy();
        }
    }

    public void notifyFinishDisplayTransitionLocked() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.dealDisplayTransition();
    }

    public void screenTurningOn() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.notifyReleaseWindow();
    }

    public void screenTurningOff() {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.notifyReleaseWindow();
    }

    public void setDeviceStateLocked(int state) {
        MiuiFoldPolicy miuiFoldPolicy;
        if (!this.mBootCompleted || (miuiFoldPolicy = this.mMiuiFoldPolicy) == null) {
            return;
        }
        miuiFoldPolicy.setDeviceStateLocked(state);
    }
}
