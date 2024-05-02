package com.android.server.display;

import android.app.UiModeManager;
import android.cameracovered.MiuiCameraCoveredManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.IntProperty;
import android.util.Slog;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.display.MiuiRampAnimator;
import com.android.server.display.expertmode.ExpertData;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowProcessController;
import java.util.HashMap;
import miui.hardware.display.DisplayFeatureManager;
import miui.hardware.display.IDisplayFeatureCallback;
import miui.os.DeviceFeature;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class ScreenEffectService extends SystemService {
    private static final int CLASSIC_READING_MODE = 0;
    private static final int MSG_SEND_HBM_STATE = 30;
    private static final int MSG_SEND_MURA_STATE = 40;
    private static final int MSG_SET_COLOR_MODE = 13;
    private static final int MSG_SET_DC_PARSE_STATE = 14;
    private static final int MSG_SET_GRAY_VALUE = 3;
    private static final int MSG_SET_PAPER_COLOR_TYPE = 16;
    private static final int MSG_SET_SECONDARY_FRAME_RATE = 15;
    private static final int MSG_SWITCH_DARK_MODE = 10;
    private static final int MSG_UPDATE_COLOR_SCHEME = 2;
    private static final int MSG_UPDATE_DFPS_MODE = 11;
    private static final int MSG_UPDATE_EXPERT_MODE = 12;
    private static final int MSG_UPDATE_HDR_STATE = 20;
    private static final int MSG_UPDATE_PCC_LEVEL = 7;
    private static final int MSG_UPDATE_READING_MODE = 1;
    private static final int MSG_UPDATE_SMART_DFPS_MODE = 17;
    private static final int MSG_UPDATE_TRUETONE = 19;
    private static final int MSG_UPDATE_UNLIMITED_COLOR_LEVEL = 8;
    private static final int MSG_UPDATE_USERCHANGE = 21;
    private static final int MSG_UPDATE_WCG_STATE = 6;
    private static final int PAPER_READING_MODE = 1;
    private static final String PERSISTENT_PROPERTY_DISPLAY_COLOR = "persist.sys.sf.native_mode";
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE = 31100;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DC_PARSE_STATE = 31036;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DFPS = 31035;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_FPS_VIDEO_INFO = 31116;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_PCC = 31101;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SECONDARY_FRAME_RATE = 31121;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SET_MODE = 31023;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SMART_DFPS = 31037;
    private static final String TAG = "ScreenEffectService";
    private static final int TEMP_PAPER_MODE_LEVEL = -1;
    public static ScreenEffectManager sScreenEffectManager;
    private boolean mAutoAdjustEnable;
    private boolean mBootCompleted;
    private int mColorSchemeCTLevel;
    private int mColorSchemeModeType;
    private Context mContext;
    private DisplayFeatureManager mDisplayFeatureManager;
    private boolean mDolbyState;
    private boolean mForceDisableEyeCare;
    private boolean mGameHdrEnabled;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int mMinimumBrightnessInt;
    private int mPaperColorType;
    private MiuiRampAnimator<DisplayFeatureManager> mPaperModeAnimator;
    private int mPaperModeMinRate;
    private PowerManager mPowerManager;
    private int mReadingModeCTLevel;
    private boolean mReadingModeEnabled;
    private int mReadingModeType;
    private ContentResolver mResolver;
    private SettingsObserver mSettingsObserver;
    private int mTrueToneModeEnabled;
    private static final boolean IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT = FeatureParser.getBoolean("is_compatible_paper_and_screen_effect", false);
    private static final float PAPER_MODE_MIN_LEVEL = FeatureParser.getFloat("paper_mode_min_level", 1.0f).floatValue();
    private static final boolean SUPPORT_UNLIMITED_COLOR_MODE = MiuiSettings.ScreenEffect.SUPPORT_UNLIMITED_COLOR_MODE;
    private static final boolean SUPPORT_DISPLAY_EXPERT_MODE = ExpertData.SUPPORT_DISPLAY_EXPERT_MODE;
    private static final int SCREEN_DEFAULT_FPS = FeatureParser.getInteger("defaultFps", 0);
    private static final boolean FPS_SWITCH_DEFAULT = SystemProperties.getBoolean("ro.vendor.fps.switch.default", false);
    public static String BRIGHTNESS_THROTTLER_STATUS = "brightness_throttler_status";
    private int mDisplayState = 0;
    private float mGrayScale = Float.NaN;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks = new HashMap<>();
    private DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);

    public ScreenEffectService(Context context) {
        super(context);
        this.mContext = context;
        this.mResolver = context.getContentResolver();
        HandlerThread handlerThread = new HandlerThread("ScreenEffectThread");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new ScreenEffectHandler(this.mHandlerThread.getLooper());
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mMinimumBrightnessInt = powerManager.getMinimumScreenBrightnessSetting();
    }

    public void onStart() {
        this.mDisplayFeatureManager = DisplayFeatureManager.getInstance();
        sScreenEffectManager = new LocalService();
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION) {
                this.mPaperModeMinRate = this.mContext.getResources().getInteger(285933619);
            }
            this.mSettingsObserver = new SettingsObserver();
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_enabled"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_optimize_mode"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_color_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_texture_color_type"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_texture_level"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_auto_adjust"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_mode_type"), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_game_mode"), false, this.mSettingsObserver, -1);
            this.mContext.registerReceiver(new UserSwitchReceiver(), new IntentFilter("android.intent.action.USER_SWITCHED"));
            if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
                this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_true_tone"), false, this.mSettingsObserver, -1);
            }
            if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION) {
                IntProperty<DisplayFeatureManager> paperMode = new IntProperty<DisplayFeatureManager>("papermode") { // from class: com.android.server.display.ScreenEffectService.1
                    public void setValue(DisplayFeatureManager object, int value) {
                        if (ScreenEffectService.this.mDisplayState != 1 && (value > 0 || ScreenEffectService.this.mPaperModeAnimator.isAnimating())) {
                            object.setScreenEffect(3, value);
                        } else if (ScreenEffectService.IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && ScreenEffectService.this.mDisplayState != 1 && !ScreenEffectService.this.mReadingModeEnabled) {
                            object.setScreenEffect(3, 0);
                        }
                    }

                    public Integer get(DisplayFeatureManager object) {
                        return 0;
                    }
                };
                MiuiRampAnimator<DisplayFeatureManager> miuiRampAnimator = new MiuiRampAnimator<>(this.mDisplayFeatureManager, paperMode);
                this.mPaperModeAnimator = miuiRampAnimator;
                miuiRampAnimator.setListener(new PaperModeAnimatListener());
            }
            if (DeviceFeature.SUPPORT_DISPLAYFEATURE_CALLBACK) {
                this.mDisplayFeatureManager.registerCallback(new IDisplayFeatureCallback.Stub() { // from class: com.android.server.display.ScreenEffectService.2
                    public void displayfeatureInfoChanged(int caseId, Object... params) {
                        if (params.length > 0) {
                            if (caseId == 10000) {
                                ScreenEffectService.this.mHandler.obtainMessage(6, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 10035) {
                                ScreenEffectService.this.mHandler.obtainMessage(11, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 10037) {
                                ScreenEffectService.this.mHandler.obtainMessage(17, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 0) {
                                ScreenEffectService.this.mHandler.obtainMessage(3, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 30000) {
                                ScreenEffectService.this.mHandler.obtainMessage(13, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 40000) {
                                ScreenEffectService.this.mHandler.obtainMessage(14, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 50000) {
                                ScreenEffectService.this.mHandler.obtainMessage(20, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 10036) {
                                ScreenEffectService.this.mHandler.obtainMessage(15, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 60000) {
                                ScreenEffectService.this.mHandler.obtainMessage(30, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                            if (caseId == 70000) {
                                ScreenEffectService.this.mHandler.obtainMessage(ScreenEffectService.MSG_SEND_MURA_STATE, ((Integer) params[0]).intValue(), 0).sendToTarget();
                            }
                        }
                        if (caseId == 20000 && params.length >= 4) {
                            SomeArgs args = SomeArgs.obtain();
                            args.arg1 = params[1];
                            args.arg2 = params[2];
                            args.arg3 = params[3];
                            ScreenEffectService.this.mHandler.obtainMessage(7, ((Integer) params[0]).intValue(), 0, args).sendToTarget();
                        }
                    }
                });
            }
            this.mHandler.obtainMessage(21).sendToTarget();
        } else if (phase == 1000) {
            this.mBootCompleted = true;
            if (!FPS_SWITCH_DEFAULT) {
                notifySFDfpsMode(getScreenDpiMode(), 17);
            }
        }
    }

    private int getScreenDpiMode() {
        return SystemProperties.getInt("persist.vendor.dfps.level", SCREEN_DEFAULT_FPS);
    }

    public void setScreenEffectAll(boolean userChange) {
        setScreenEffectColor(userChange);
        if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
            this.mHandler.obtainMessage(19).sendToTarget();
        }
        this.mHandler.obtainMessage(1, true).sendToTarget();
    }

    public void setScreenEffectColor(boolean userChange) {
        boolean z = SUPPORT_UNLIMITED_COLOR_MODE;
        if (z) {
            this.mHandler.obtainMessage(8, Boolean.valueOf(userChange)).sendToTarget();
        }
        if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || z) {
            this.mHandler.obtainMessage(2, Boolean.valueOf(userChange)).sendToTarget();
        }
    }

    public void notifySFWcgState(boolean enable) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeBoolean(enable);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFDfpsMode(int mode, int msg) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                try {
                    if (msg == 17) {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SMART_DFPS, data, null, 0);
                    } else {
                        flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DFPS, data, null, 0);
                    }
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dfps mode to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFPccLevel(int level, float red, float green, float blue) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(level);
            data.writeFloat(red);
            data.writeFloat(green);
            data.writeFloat(blue);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_PCC, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFColorMode(int mode) {
        Settings.System.putIntForUser(this.mResolver, "display_color_mode", mode, -2);
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(mode);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SET_MODE, data, null, 0);
                    SystemProperties.set(PERSISTENT_PROPERTY_DISPLAY_COLOR, Integer.toString(mode));
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dfps mode to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFDCParseState(int state) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(state);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_DC_PARSE_STATE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dc parse state to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFSecondaryFrameRate(int rateState) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(rateState);
            try {
                try {
                    flinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_SECONDARY_FRAME_RATE, data, null, 0);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to notify dc parse state to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void notifySFVideoInformation(boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio) {
        IBinder surfaceFlinger = ServiceManager.getService(SURFACE_FLINGER);
        if (surfaceFlinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeBoolean(bulletChatStatus);
            data.writeFloat(frameRate);
            data.writeInt(width);
            data.writeInt(height);
            data.writeFloat(compressionRatio);
            Slog.d(TAG, "notifySFVideoInformation bulletChatStatus:" + bulletChatStatus + ", frameRate:" + frameRate + ", resolution:" + width + "x" + height + ", compressionRatio:" + compressionRatio);
            try {
                try {
                    surfaceFlinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_FEATURE_FPS_VIDEO_INFO, data, reply, 0);
                } catch (RemoteException | SecurityException | UnsupportedOperationException e) {
                    Slog.e(TAG, "notifySFVideoInformation RemoteException:" + e);
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
    }

    public void loadSettings() {
        updateReadingModeEnable();
        updateReadingModeType();
        updatePaperColorType();
        resetLocalPaperLevelIfNeed();
        updateAutoAdjustEnable();
        updateColorSchemeModeType();
        updateColorSchemeCTLevel();
        if (MiuiSettings.ScreenEffect.SUPPORT_TRUETONE_MODE) {
            updateTrueToneModeEnable();
        }
    }

    public void updateTrueToneModeEnable() {
        this.mTrueToneModeEnabled = Settings.System.getIntForUser(this.mResolver, "screen_true_tone", 0, -2);
    }

    public void updatePaperColorType() {
        this.mPaperColorType = Settings.System.getIntForUser(this.mResolver, "screen_texture_color_type", 0, -2);
    }

    public void updateColorSchemeCTLevel() {
        this.mColorSchemeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_color_level", 2, -2);
    }

    public void updateColorSchemeModeType() {
        this.mColorSchemeModeType = Settings.System.getIntForUser(this.mResolver, "screen_optimize_mode", MiuiSettings.ScreenEffect.DEFAULT_SCREEN_OPTIMIZE_MODE, -2);
    }

    public void updateAutoAdjustEnable() {
        if (!isSupportSmartEyeCare()) {
            return;
        }
        boolean z = false;
        if (Settings.System.getIntForUser(this.mResolver, "screen_auto_adjust", 0, -2) != 0) {
            z = true;
        }
        this.mAutoAdjustEnable = z;
    }

    public void updateReadingModeEnable() {
        boolean z = false;
        if (Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_enabled", 0, -2) != 0) {
            z = true;
        }
        this.mReadingModeEnabled = z;
    }

    public void updateReadingModeType() {
        this.mReadingModeType = Settings.System.getIntForUser(this.mResolver, "screen_mode_type", 0, -2);
        updateReadingModeCTLevel();
    }

    public void updateClassicReadingModeCTLevel() {
        this.mReadingModeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_level", MiuiSettings.ScreenEffect.DEFAULT_PAPER_MODE_LEVEL, -2);
    }

    public void updatePaperReadingModeCTLevel() {
        this.mReadingModeCTLevel = Settings.System.getIntForUser(this.mResolver, "screen_paper_texture_level", (int) MiuiSettings.ScreenEffect.DEFAULT_TEXTURE_MODE_LEVEL, -2);
    }

    private void updateReadingModeCTLevel() {
        if (this.mReadingModeType == 0) {
            updateClassicReadingModeCTLevel();
        } else {
            updatePaperReadingModeCTLevel();
        }
    }

    public void updateVideoInformationIfNeeded(int pid, boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio, IBinder token) {
        if (!isForegroundApp(pid)) {
            return;
        }
        setDeathCallbackLocked(token, true);
        notifySFVideoInformation(bulletChatStatus, frameRate, width, height, compressionRatio);
    }

    private boolean isForegroundApp(int pid) {
        ActivityTaskManagerInternal mAtmInternal = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        WindowProcessController wpc = mAtmInternal != null ? mAtmInternal.getTopApp() : null;
        int topAppPid = wpc != null ? wpc.getPid() : 0;
        if (pid != topAppPid) {
            return false;
        }
        return true;
    }

    private void setDeathCallbackLocked(IBinder token, boolean register) {
        if (register) {
            registerDeathCallbackLocked(token);
        } else {
            unregisterDeathCallbackLocked(token);
        }
    }

    protected void registerDeathCallbackLocked(IBinder token) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            return;
        }
        synchronized (this.mClientDeathCallbacks) {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        if (token != null) {
            synchronized (this.mClientDeathCallbacks) {
                ClientDeathCallback deathCallback = this.mClientDeathCallbacks.remove(token);
                if (deathCallback != null) {
                    token.unlinkToDeath(deathCallback, 0);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private IBinder mToken;

        public ClientDeathCallback(IBinder token) {
            ScreenEffectService.this = this$0;
            try {
                this.mToken = token;
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(ScreenEffectService.TAG, "binderDied...");
            synchronized (ScreenEffectService.this.mClientDeathCallbacks) {
                ScreenEffectService.this.mClientDeathCallbacks.remove(this.mToken);
            }
            this.mToken.unlinkToDeath(this, 0);
            ScreenEffectService.this.notifySFVideoInformation(false, 0.0f, 0, 0, 0.0f);
        }
    }

    private void resetLocalPaperLevelIfNeed() {
        if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && this.mReadingModeCTLevel < PAPER_MODE_MIN_LEVEL) {
            int tempValue = Settings.System.getIntForUser(this.mResolver, "screen_paper_mode_level", -1, -2);
            if (tempValue != -1) {
                int i = MiuiSettings.ScreenEffect.DEFAULT_PAPER_MODE_LEVEL;
                this.mReadingModeCTLevel = i;
                Settings.System.putIntForUser(this.mResolver, "screen_paper_mode_level", i, -2);
            }
        }
    }

    /* loaded from: classes.dex */
    private class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver() {
            super(r1.mHandler);
            ScreenEffectService.this = r1;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -1671532134:
                    if (lastPathSegment.equals("screen_paper_texture_level")) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case -1618391570:
                    if (lastPathSegment.equals("screen_paper_mode_level")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1457819203:
                    if (lastPathSegment.equals("screen_game_mode")) {
                        c = 5;
                        break;
                    }
                    c = 65535;
                    break;
                case -1111615120:
                    if (lastPathSegment.equals("screen_true_tone")) {
                        c = '\t';
                        break;
                    }
                    c = 65535;
                    break;
                case -185483197:
                    if (lastPathSegment.equals("screen_mode_type")) {
                        c = '\b';
                        break;
                    }
                    c = 65535;
                    break;
                case 632817389:
                    if (lastPathSegment.equals("screen_texture_color_type")) {
                        c = 7;
                        break;
                    }
                    c = 65535;
                    break;
                case 671593557:
                    if (lastPathSegment.equals("screen_color_level")) {
                        c = 4;
                        break;
                    }
                    c = 65535;
                    break;
                case 1878999244:
                    if (lastPathSegment.equals("screen_auto_adjust")) {
                        c = 6;
                        break;
                    }
                    c = 65535;
                    break;
                case 1962624818:
                    if (lastPathSegment.equals("screen_optimize_mode")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case 2119453483:
                    if (lastPathSegment.equals("screen_paper_mode_enabled")) {
                        c = 0;
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
                    ScreenEffectService.this.updateReadingModeEnable();
                    ScreenEffectService.this.handleReadingModeChange(false);
                    return;
                case 1:
                    if (ScreenEffectService.this.mReadingModeEnabled && ScreenEffectService.this.mReadingModeType == 0) {
                        ScreenEffectService.this.updateClassicReadingModeCTLevel();
                        ScreenEffectService.this.updatePaperMode(true, true);
                        return;
                    }
                    return;
                case 2:
                    if (ScreenEffectService.this.mReadingModeEnabled && ScreenEffectService.this.mReadingModeType == 1) {
                        ScreenEffectService.this.updatePaperReadingModeCTLevel();
                        ScreenEffectService.this.updatePaperMode(true, true);
                        return;
                    }
                    return;
                case 3:
                    ScreenEffectService.this.updateColorSchemeModeType();
                    ScreenEffectService.this.handleScreenSchemeChange(false);
                    return;
                case 4:
                    ScreenEffectService.this.updateColorSchemeCTLevel();
                    if (ScreenEffectService.SUPPORT_UNLIMITED_COLOR_MODE) {
                        ScreenEffectService.this.handleUnlimitedColorLevelChange(false);
                        return;
                    } else {
                        ScreenEffectService.this.handleScreenSchemeChange(false);
                        return;
                    }
                case 5:
                    ScreenEffectService.this.handleGameModeChange();
                    return;
                case 6:
                    ScreenEffectService.this.updateAutoAdjustEnable();
                    if (ScreenEffectService.this.isSupportSmartEyeCare() && ScreenEffectService.this.mReadingModeEnabled) {
                        ScreenEffectService.this.handleAutoAdjustChange();
                        return;
                    }
                    return;
                case 7:
                    ScreenEffectService.this.updatePaperColorType();
                    if (ScreenEffectService.this.mReadingModeEnabled && ScreenEffectService.this.mReadingModeType == 1) {
                        ScreenEffectService screenEffectService = ScreenEffectService.this;
                        screenEffectService.setPaperColors(screenEffectService.mReadingModeType);
                        return;
                    }
                    return;
                case '\b':
                    ScreenEffectService.this.updateReadingModeType();
                    if (ScreenEffectService.this.mReadingModeEnabled) {
                        if (!ScreenEffectService.this.mAutoAdjustEnable) {
                            ScreenEffectService.this.updatePaperMode(true, false);
                        }
                        ScreenEffectService screenEffectService2 = ScreenEffectService.this;
                        screenEffectService2.setPaperColors(screenEffectService2.mReadingModeType);
                        return;
                    }
                    return;
                case '\t':
                    ScreenEffectService.this.updateTrueToneModeEnable();
                    ScreenEffectService.this.handleTrueToneModeChange();
                    return;
                default:
                    return;
            }
        }
    }

    public void handleReadingModeChange(boolean immediate) {
        if (this.mReadingModeEnabled) {
            setPaperColors(this.mReadingModeType);
            if (this.mAutoAdjustEnable) {
                handleAutoAdjustChange();
                return;
            } else {
                updatePaperMode(true, immediate);
                return;
            }
        }
        setPaperColors(0);
        updatePaperMode(false, this.mAutoAdjustEnable);
    }

    /* loaded from: classes.dex */
    private class ScreenEffectHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ScreenEffectHandler(Looper looper) {
            super(looper);
            ScreenEffectService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean z = false;
            boolean z2 = false;
            int i = 0;
            switch (msg.what) {
                case 1:
                    ScreenEffectService.this.handleReadingModeChange(((Boolean) msg.obj).booleanValue());
                    return;
                case 2:
                    ScreenEffectService.this.handleScreenSchemeChange(((Boolean) msg.obj).booleanValue());
                    return;
                case 3:
                    ScreenEffectService.this.mGrayScale = (msg.arg1 * 1.0f) / 255.0f;
                    ScreenEffectService.this.notifyGrayScaleChanged();
                    return;
                case 6:
                    ScreenEffectService screenEffectService = ScreenEffectService.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    screenEffectService.notifySFWcgState(z);
                    return;
                case 7:
                    SomeArgs args = (SomeArgs) msg.obj;
                    float red = ((Float) args.arg1).floatValue();
                    float green = ((Float) args.arg2).floatValue();
                    float blue = ((Float) args.arg3).floatValue();
                    ScreenEffectService.this.notifySFPccLevel(msg.arg1, red, green, blue);
                    args.recycle();
                    return;
                case 8:
                    ScreenEffectService.this.handleUnlimitedColorLevelChange(((Boolean) msg.obj).booleanValue());
                    return;
                case 10:
                    ScreenEffectService screenEffectService2 = ScreenEffectService.this;
                    Context context = screenEffectService2.mContext;
                    ScreenEffectService screenEffectService3 = ScreenEffectService.this;
                    screenEffectService2.setDarkModeEnable(context, screenEffectService3.isDarkModeEnable(screenEffectService3.mContext));
                    return;
                case 11:
                case 17:
                    ScreenEffectService.this.notifySFDfpsMode(msg.arg1, msg.what);
                    return;
                case 12:
                    ScreenEffectService.this.setExpertScreenMode();
                    return;
                case 13:
                    ScreenEffectService.this.notifySFColorMode(msg.arg1);
                    return;
                case 14:
                    ScreenEffectService.this.notifySFDCParseState(msg.arg1);
                    return;
                case 15:
                    ScreenEffectService.this.notifySFSecondaryFrameRate(msg.arg1);
                    return;
                case 16:
                    ScreenEffectService screenEffectService4 = ScreenEffectService.this;
                    if (screenEffectService4.mReadingModeEnabled) {
                        i = ScreenEffectService.this.mReadingModeType;
                    }
                    screenEffectService4.setPaperColors(i);
                    return;
                case 19:
                    ScreenEffectService.this.handleTrueToneModeChange();
                    return;
                case 20:
                    ScreenEffectService screenEffectService5 = ScreenEffectService.this;
                    if (msg.arg1 == 2) {
                        z2 = true;
                    }
                    screenEffectService5.mDolbyState = z2;
                    Settings.System.putIntForUser(ScreenEffectService.this.mContext.getContentResolver(), ScreenEffectService.BRIGHTNESS_THROTTLER_STATUS, ScreenEffectService.this.mDolbyState ? 1 : 0, -2);
                    ScreenEffectService.this.notifyHdrStateChanged();
                    return;
                case 21:
                    Settings.System.putInt(ScreenEffectService.this.mResolver, "screen_game_mode", 0);
                    ScreenEffectService.this.loadSettings();
                    ScreenEffectService.this.setScreenEffectAll(true);
                    return;
                case 30:
                    ScreenEffectService.this.sendHbmState(msg.arg1);
                    return;
                case ScreenEffectService.MSG_SEND_MURA_STATE /* 40 */:
                    ScreenEffectService.this.sendMuraState(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    public void sendHbmState(int type) {
        MiuiCameraCoveredManager.hbmCoveredAnimation(type);
    }

    public void sendMuraState(int type) {
        MiuiCameraCoveredManager.cupMuraCoveredAnimation(type);
    }

    public void updatePaperMode(boolean enabled, boolean immediate) {
        if (this.mForceDisableEyeCare) {
            return;
        }
        setScreenEyeCare(enabled, immediate);
    }

    public void handleAutoAdjustChange() {
        if (this.mAutoAdjustEnable) {
            this.mDisplayFeatureManager.setScreenEffect(3, 256);
            return;
        }
        updateReadingModeCTLevel();
        updatePaperMode(true, true);
    }

    public boolean isSupportSmartEyeCare() {
        return FeatureParser.getBoolean("support_smart_eyecare", false);
    }

    public void setPaperColors(int type) {
        this.mDisplayFeatureManager.setScreenEffect(31, type == 0 ? 0 : this.mPaperColorType);
    }

    private void setScreenEyeCare(boolean enabled, boolean immediate) {
        if (DeviceFeature.SUPPORT_PAPERMODE_ANIMATION && this.mPaperModeAnimator != null && (immediate || this.mDisplayState != 1)) {
            int rate = immediate ? 0 : Math.max((this.mReadingModeCTLevel * 2) / 3, this.mPaperModeMinRate);
            int targetLevel = enabled ? this.mReadingModeCTLevel : 0;
            if (this.mPaperModeAnimator.animateTo(targetLevel, rate)) {
                return;
            }
        }
        if (enabled) {
            this.mDisplayFeatureManager.setScreenEffect(3, this.mReadingModeCTLevel);
        } else if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT) {
            this.mDisplayFeatureManager.setScreenEffect(3, 0);
        } else {
            handleScreenSchemeChange(false);
        }
    }

    public void handleScreenSchemeChange(boolean userChange) {
        if (!IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT && ((this.mReadingModeEnabled || this.mGameHdrEnabled) && !this.mForceDisableEyeCare)) {
            return;
        }
        int value = this.mColorSchemeCTLevel;
        int mode = 0;
        int i = this.mColorSchemeModeType;
        if (i == 2) {
            mode = 1;
        } else if (i == 3) {
            mode = 2;
        } else if (i == 4) {
            if (!this.mBootCompleted || userChange) {
                setExpertScreenMode();
                return;
            }
            return;
        }
        this.mDisplayFeatureManager.setScreenEffect(mode, value);
    }

    public void handleGameModeChange() {
        int gameMode = Settings.System.getIntForUser(this.mResolver, "screen_game_mode", 0, -2);
        int gameHdrLevel = Settings.System.getIntForUser(this.mResolver, "game_hdr_level", 0, -2);
        boolean z = true;
        boolean gameHdrEnabled = (gameMode & 2) != 0;
        boolean forceDisableEyecare = (gameMode & 1) != 0;
        if (this.mGameHdrEnabled != gameHdrEnabled) {
            this.mGameHdrEnabled = gameHdrEnabled;
            if (!gameHdrEnabled) {
                this.mDisplayFeatureManager.setScreenEffect(19, 0);
                handleScreenSchemeChange(false);
            } else if (IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || ((forceDisableEyecare && this.mReadingModeEnabled) || !this.mReadingModeEnabled)) {
                if (forceDisableEyecare && this.mReadingModeEnabled) {
                    setScreenEyeCare(false, true);
                }
                this.mDisplayFeatureManager.setScreenEffect(19, gameHdrLevel);
            }
        }
        if (this.mForceDisableEyeCare != forceDisableEyecare) {
            this.mForceDisableEyeCare = forceDisableEyecare;
        }
        if ((IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT || !gameHdrEnabled) && this.mReadingModeEnabled) {
            if (this.mAutoAdjustEnable) {
                if (!forceDisableEyecare) {
                    handleAutoAdjustChange();
                    return;
                } else {
                    setScreenEyeCare(false, true);
                    return;
                }
            }
            if (forceDisableEyecare) {
                z = false;
            }
            setScreenEyeCare(z, false);
        }
    }

    public void handleUnlimitedColorLevelChange(boolean userChange) {
        if (this.mColorSchemeModeType != 4 || !this.mBootCompleted || userChange) {
            this.mDisplayFeatureManager.setScreenEffect(23, this.mColorSchemeCTLevel);
        }
    }

    public void handleTrueToneModeChange() {
        this.mDisplayFeatureManager.setScreenEffect(32, this.mTrueToneModeEnabled);
    }

    /* loaded from: classes.dex */
    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
            ScreenEffectService.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!ScreenEffectService.this.mBootCompleted) {
                return;
            }
            ScreenEffectService.this.mHandler.obtainMessage(21).sendToTarget();
            if (Build.VERSION.SDK_INT >= 29) {
                ScreenEffectService.this.mHandler.removeMessages(10);
                ScreenEffectService.this.mHandler.obtainMessage(10).sendToTarget();
            }
        }
    }

    public void setDarkModeEnable(Context ctx, boolean enable) {
        UiModeManager manager = (UiModeManager) ctx.getSystemService(UiModeManager.class);
        if (manager == null) {
            return;
        }
        manager.setNightMode(enable ? 2 : 1);
    }

    public boolean isDarkModeEnable(Context ctx) {
        return 2 == Settings.Secure.getIntForUser(ctx.getContentResolver(), "ui_night_mode", 1, 0);
    }

    /* loaded from: classes.dex */
    class LocalService extends ScreenEffectManager {
        LocalService() {
            ScreenEffectService.this = this$0;
        }

        @Override // com.android.server.display.ScreenEffectManager
        public void updateScreenEffect(int state) {
            int oldState = ScreenEffectService.this.mDisplayState;
            ScreenEffectService.this.mDisplayState = state;
            if (!DeviceFeature.PERSIST_SCREEN_EFFECT && oldState == 1 && state != oldState) {
                ScreenEffectService.this.setScreenEffectColor(false);
            }
        }

        @Override // com.android.server.display.ScreenEffectManager
        public void updateDozeBrightness(long physicalDisplayId, int brightness) {
            ScreenEffectService.this.mDisplayFeatureManager.setDozeBrightness(physicalDisplayId, brightness, ScreenEffectService.this.mMinimumBrightnessInt);
        }

        @Override // com.android.server.display.ScreenEffectManager
        public void updateBCBCState(int state) {
            ScreenEffectService.this.mDisplayFeatureManager.setScreenEffect(18, state);
        }

        @Override // com.android.server.display.ScreenEffectManager
        public void setVideoInformation(int pid, boolean bulletChatStatus, float frameRate, int width, int height, float compressionRatio, IBinder token) {
            ScreenEffectService.this.updateVideoInformationIfNeeded(pid, bulletChatStatus, frameRate, width, height, compressionRatio, token);
        }
    }

    public static void startScreenEffectService() {
        if (MiuiSettings.ScreenEffect.SCREEN_EFFECT_SUPPORTED != 0 && sScreenEffectManager == null) {
            SystemServiceManager systemServiceManager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
            systemServiceManager.startService(ScreenEffectService.class);
        }
    }

    /* loaded from: classes.dex */
    class PaperModeAnimatListener implements MiuiRampAnimator.Listener {
        PaperModeAnimatListener() {
            ScreenEffectService.this = this$0;
        }

        @Override // com.android.server.display.MiuiRampAnimator.Listener
        public void onAnimationEnd() {
            if (ScreenEffectService.this.mDisplayState != 1 && !ScreenEffectService.IS_COMPATIBLE_PAPER_AND_SCREEN_EFFECT) {
                ScreenEffectService.this.handleScreenSchemeChange(false);
            }
        }
    }

    public void notifyGrayScaleChanged() {
        Bundle bundle = new Bundle();
        bundle.putFloat("gray_scale", this.mGrayScale);
        this.mDisplayManagerInternal.notifyDisplayManager(0, 1, bundle);
    }

    public void notifyHdrStateChanged() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("dolby_version_state", this.mDolbyState);
        this.mDisplayManagerInternal.notifyDisplayManager(0, 3, bundle);
    }

    private ExpertData getExpertData(Context context) {
        ExpertData data = ExpertData.getFromDatabase(context);
        if (data == null) {
            return ExpertData.getDefaultValue();
        }
        return data;
    }

    public void setExpertScreenMode() {
        if (!SUPPORT_DISPLAY_EXPERT_MODE) {
            Slog.w(TAG, "device don't support DISPLAY_EXPERT_MODE");
        }
        ExpertData data = getExpertData(this.mContext);
        if (data == null) {
            return;
        }
        for (int cookie = 0; cookie < 9; cookie++) {
            this.mDisplayFeatureManager.setScreenEffect(26, data.getByCookie(cookie), cookie);
        }
    }
}
