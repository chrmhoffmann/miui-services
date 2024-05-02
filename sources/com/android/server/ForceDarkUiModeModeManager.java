package com.android.server;

import android.app.IAppDarkModeObserver;
import android.app.UiModeManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.view.ForceDarkHelperStub;
import com.android.server.ForceDarkAppConfigProvider;
import com.android.server.policy.DisplayTurnoverManager;
import com.miui.darkmode.DarkModeAppData;
import java.io.PrintWriter;
import java.util.HashMap;
/* loaded from: classes.dex */
public class ForceDarkUiModeModeManager {
    private static final String TAG = UiModeManager.class.getSimpleName();
    private DarkModeTimeModeManager mDarkModeTimeModeManager;
    private ForceDarkAppConfigProvider mForceDarkAppConfigProvider;
    private ForceDarkAppListManager mForceDarkAppListManager;
    private HashMap<String, String> mForceDarkConfigData;
    private UiModeManagerService mUiModeManagerService;
    final RemoteCallbackList<IAppDarkModeObserver> mAppDarkModeObservers = new RemoteCallbackList<>();
    private int mForceDarkDebug = SystemProperties.getInt("sys.forcedark.debug", 0);

    public ForceDarkUiModeModeManager(UiModeManagerService uiModeManagerService) {
        this.mUiModeManagerService = uiModeManagerService;
        initForceDarkAppConfig();
        this.mForceDarkAppListManager = new ForceDarkAppListManager(uiModeManagerService.getContext(), this);
        this.mDarkModeTimeModeManager = new DarkModeTimeModeManager(this.mUiModeManagerService.getContext());
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        switch (code) {
            case 16777207:
                data.enforceInterface("android.app.IUiModeManager");
                reply.writeNoException();
                HashMap<String, Integer> forceDarkForSplashScreen = this.mForceDarkAppListManager.getAllForceDarkMapForSplashScreen();
                reply.writeMap(forceDarkForSplashScreen);
                return true;
            case 16777208:
                data.enforceInterface("android.app.IUiModeManager");
                String packageName = data.readString();
                int userId = data.readInt();
                reply.writeNoException();
                boolean shouldIntercept = this.mForceDarkAppListManager.getShouldInterceptRelaunch(packageName, userId);
                reply.writeBoolean(shouldIntercept);
                return true;
            case 16777209:
                data.enforceInterface("android.app.IUiModeManager");
                String packageName2 = data.readString();
                reply.writeNoException();
                boolean forceDarkOriginState = this.mForceDarkAppListManager.getAppForceDarkOrigin(packageName2);
                reply.writeBoolean(forceDarkOriginState);
                return true;
            case 16777210:
                data.enforceInterface("android.app.IUiModeManager");
                long appLastUpdateTime = data.readLong();
                int userId2 = data.readInt();
                reply.writeNoException();
                DarkModeAppData darkModeAppData = this.mForceDarkAppListManager.getDarkModeAppList(appLastUpdateTime, userId2);
                reply.writeParcelable(darkModeAppData, 0);
                return true;
            case DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY /* 16777211 */:
                data.enforceInterface("android.app.IUiModeManager");
                String packageName3 = data.readString();
                reply.writeNoException();
                String config = getForceDarkAppConfig(packageName3);
                if (TextUtils.isEmpty(config)) {
                    Slog.i(TAG, "systemserver package:" + packageName3 + " null");
                } else {
                    Slog.i(TAG, "systemserver package:" + packageName3 + " configLength:" + config.length());
                }
                reply.writeString(config);
                return true;
            case DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY /* 16777212 */:
                data.enforceInterface("android.app.IUiModeManager");
                registerAppDarkModeCallback(IAppDarkModeObserver.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readInt(), data.readInt());
                reply.writeNoException();
                return true;
            case 16777213:
                data.enforceInterface("android.app.IUiModeManager");
                String packageName4 = data.readString();
                int userId3 = data.readInt();
                boolean appDarkModeEnable = this.mForceDarkAppListManager.getAppDarkModeEnable(packageName4, userId3);
                reply.writeNoException();
                reply.writeBoolean(appDarkModeEnable);
                return true;
            case 16777214:
                data.enforceInterface("android.app.IUiModeManager");
                String packageName5 = data.readString();
                boolean enable = data.readBoolean();
                int userId4 = data.readInt();
                setAppDarkModeEnable(packageName5, enable, userId4);
                reply.writeNoException();
                return true;
            default:
                return false;
        }
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            Log.i("forceDarkCouldData", "onBootPhase activity manager ready");
            this.mForceDarkAppListManager.onBootPhase(phase, this.mUiModeManagerService.getContext());
            this.mForceDarkAppConfigProvider.onBootPhase(phase, this.mUiModeManagerService.getContext());
            ForceDarkHelperStub.getInstance().initialize(this.mUiModeManagerService.getContext());
            this.mDarkModeTimeModeManager.onBootPhase(this.mUiModeManagerService.getContext());
            DarkModeStatusTracker.getIntance().init(this.mUiModeManagerService.getContext(), this.mForceDarkAppListManager);
        }
    }

    public void setAppDarkModeEnable(String packageName, boolean enable, int userId) {
        if (this.mUiModeManagerService.getContext().checkCallingOrSelfPermission("android.permission.MODIFY_DAY_NIGHT_MODE") != 0) {
            Slog.e(TAG, "setAppDarkModeEnable failed, requires MODIFY_DAY_NIGHT_MODE permission");
            return;
        }
        this.mForceDarkAppListManager.setAppDarkModeEnable(packageName, enable, userId);
        synchronized (this.mUiModeManagerService.getInnerLock()) {
            int i = this.mAppDarkModeObservers.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    AppDarkModeObserverRegistration registration = (AppDarkModeObserverRegistration) this.mAppDarkModeObservers.getBroadcastCookie(i2);
                    if (registration.mPackageName != null && packageName != null && registration.mPackageName.equals(packageName)) {
                        IAppDarkModeObserver observer = this.mAppDarkModeObservers.getBroadcastItem(i2);
                        try {
                            observer.onAppDarkModeChanged(enable);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    i = i2;
                } else {
                    this.mAppDarkModeObservers.finishBroadcast();
                }
            }
        }
    }

    public void registerAppDarkModeCallback(IAppDarkModeObserver observer, String packageName, int processId, int userId) {
        if (observer == null || TextUtils.isEmpty(packageName)) {
            Slog.i(TAG, "registAppDarkModeCallback param is null");
            return;
        }
        synchronized (this.mUiModeManagerService.getInnerLock()) {
            int i = this.mAppDarkModeObservers.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    AppDarkModeObserverRegistration registration = (AppDarkModeObserverRegistration) this.mAppDarkModeObservers.getBroadcastCookie(i2);
                    if (registration.mPackageName.equals(packageName) && registration.mProcessId == processId && registration.mUserId == userId) {
                        RemoteCallbackList<IAppDarkModeObserver> remoteCallbackList = this.mAppDarkModeObservers;
                        remoteCallbackList.unregister(remoteCallbackList.getBroadcastItem(i2));
                    }
                    i = i2;
                } else {
                    this.mAppDarkModeObservers.finishBroadcast();
                    this.mAppDarkModeObservers.register(observer, new AppDarkModeObserverRegistration(packageName, processId, userId));
                }
            }
        }
    }

    public void dump(PrintWriter pw) {
        int startTime;
        int endTime;
        if (this.mForceDarkDebug == 0) {
            return;
        }
        pw.println("ForceDarkUiModeManager");
        boolean z = false;
        if (SystemProperties.getInt("sys.debug.darkmode.analysis", 0) == 1) {
            z = true;
        }
        DarkModeStatusTracker.DEBUG = z;
        pw.println(" time mode info:");
        pw.print(" time mode enable =" + DarkModeTimeModeHelper.isDarkModeTimeEnable(this.mUiModeManagerService.getContext()));
        if (DarkModeTimeModeHelper.isDarkModeTimeEnable(this.mUiModeManagerService.getContext()) && (!DarkModeTimeModeHelper.isInternationalVersion() || !DarkModeTimeModeHelper.isSuntimeType(this.mUiModeManagerService.getContext()))) {
            if (DarkModeTimeModeHelper.isSuntimeType(this.mUiModeManagerService.getContext())) {
                startTime = DarkModeTimeModeHelper.getSunSetTime(this.mUiModeManagerService.getContext());
            } else {
                startTime = DarkModeTimeModeHelper.getDarkModeStartTime(this.mUiModeManagerService.getContext());
            }
            if (DarkModeTimeModeHelper.isSuntimeType(this.mUiModeManagerService.getContext())) {
                endTime = DarkModeTimeModeHelper.getSunRiseTime(this.mUiModeManagerService.getContext());
            } else {
                endTime = DarkModeTimeModeHelper.getDarkModeEndTime(this.mUiModeManagerService.getContext());
            }
            pw.println(" type=" + DarkModeTimeModeHelper.getDarkModeTimeType(this.mUiModeManagerService.getContext()) + " startTime = " + DarkModeTimeModeHelper.getTimeInString(startTime) + " endTime = " + DarkModeTimeModeHelper.getTimeInString(endTime));
        }
        synchronized (this.mUiModeManagerService.getInnerLock()) {
            int i = this.mAppDarkModeObservers.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    AppDarkModeObserverRegistration registration = (AppDarkModeObserverRegistration) this.mAppDarkModeObservers.getBroadcastCookie(i2);
                    if (registration.mPackageName != null) {
                        IAppDarkModeObserver observer = this.mAppDarkModeObservers.getBroadcastItem(i2);
                        try {
                            Bundle bundle = new Bundle();
                            observer.onDump(bundle);
                            pw.println("package: " + registration.mPackageName);
                            for (String key : bundle.keySet()) {
                                pw.println(key + ": " + bundle.getString(key));
                            }
                            pw.println();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    i = i2;
                } else {
                    this.mAppDarkModeObservers.finishBroadcast();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class AppDarkModeObserverRegistration {
        String mPackageName;
        int mProcessId;
        int mUserId;

        AppDarkModeObserverRegistration(String packageName, int processId, int userId) {
            this.mPackageName = packageName;
            this.mProcessId = processId;
            this.mUserId = userId;
        }
    }

    private void dumpMiuiUiModeManagerStub() {
        if (this.mUiModeManagerService.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            return;
        }
        Slog.d(UiModeManager.class.getSimpleName(), "mAppDarkModeObservers callback size = " + this.mAppDarkModeObservers.getRegisteredCallbackCount());
    }

    private void initForceDarkAppConfig() {
        ForceDarkAppConfigProvider forceDarkAppConfigProvider = ForceDarkAppConfigProvider.getInstance();
        this.mForceDarkAppConfigProvider = forceDarkAppConfigProvider;
        forceDarkAppConfigProvider.setAppConfigChangeListener(new ForceDarkAppConfigProvider.ForceDarkAppConfigChangeListener() { // from class: com.android.server.ForceDarkUiModeModeManager.1
            @Override // com.android.server.ForceDarkAppConfigProvider.ForceDarkAppConfigChangeListener
            public void onChange() {
                Log.i(ForceDarkUiModeModeManager.TAG, "onForceDarkConfigChanged");
                ForceDarkHelperStub forceDarkHelperStub = ForceDarkHelperStub.getInstance();
                ForceDarkUiModeModeManager forceDarkUiModeModeManager = ForceDarkUiModeModeManager.this;
                forceDarkHelperStub.onForceDarkConfigChanged(forceDarkUiModeModeManager.getForceDarkAppConfig(forceDarkUiModeModeManager.mUiModeManagerService.getContext().getPackageName()));
            }
        });
    }

    public String getForceDarkAppConfig(String packageName) {
        return this.mForceDarkAppConfigProvider.getForceDarkAppConfig(packageName);
    }

    public void notifyAppListChanged() {
        synchronized (this.mUiModeManagerService.getInnerLock()) {
            int i = this.mAppDarkModeObservers.beginBroadcast();
            while (true) {
                int i2 = i - 1;
                if (i > 0) {
                    AppDarkModeObserverRegistration registration = (AppDarkModeObserverRegistration) this.mAppDarkModeObservers.getBroadcastCookie(i2);
                    if (isSystemUiProcess(registration.mPackageName)) {
                        IAppDarkModeObserver observer = this.mAppDarkModeObservers.getBroadcastItem(i2);
                        try {
                            HashMap<String, Integer> forceDarkForSplashScreen = this.mForceDarkAppListManager.getAllForceDarkMapForSplashScreen();
                            observer.onAppSplashScreenChanged(forceDarkForSplashScreen);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    i = i2;
                } else {
                    this.mAppDarkModeObservers.finishBroadcast();
                }
            }
        }
    }

    private boolean isSystemUiProcess(String packageName) {
        return "com.android.systemui".equals(packageName);
    }
}
