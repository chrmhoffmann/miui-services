package com.android.server;

import android.appcompat.ApplicationCompatUtilsStub;
import android.content.Context;
import android.magicpointer.util.MiuiMagicPointerUtilsStubHead;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.MiuiAppSizeCompatModeImpl;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.MiuiMemoryService;
import com.android.server.am.ProcessManagerService;
import com.android.server.am.SmartPowerService;
import com.android.server.cameracovered.MiuiCameraCoveredManagerService;
import com.android.server.display.ScreenEffectService;
import com.android.server.input.InputManagerService;
import com.android.server.input.MiuiInputManagerService;
import com.android.server.input.shoulderkey.ShoulderKeyManagerService;
import com.android.server.lights.MiuiLightsService;
import com.android.server.pm.Installer;
import com.android.server.policy.MiuiPhoneWindowManager;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.wm.AppContinuityRouterStub;
import com.android.server.wm.ApplicationCompatRouterStub;
import com.android.server.wm.MiuiEmbeddingWindowServiceStubHead;
import com.android.server.wm.MiuiMagicPointerServiceStub;
import com.android.server.wm.MiuiMagicPointerServiceStubHead;
import com.android.server.wm.MiuiSizeCompatService;
import com.android.server.wm.WindowManagerService;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.annotations.MiuiStubHead;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.BackupManagerService;
import com.miui.server.MiuiCldService;
import com.miui.server.MiuiDfcService;
import com.miui.server.MiuiFboService;
import com.miui.server.MiuiInitServer;
import com.miui.server.MiuiWebViewManagerService;
import com.miui.server.PerfShielderService;
import com.miui.server.SecurityManagerService;
import com.miui.server.car.MiuiCarServiceStub;
import com.miui.server.enterprise.EnterpriseManagerService;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.server.migard.MiGardService;
import com.miui.server.rescue.BrokenScreenRescueService;
import com.miui.server.rtboost.SchedBoostService;
import com.miui.server.sentinel.MiuiSentinelService;
import com.miui.server.stepcounter.StepCounterManagerService;
import com.miui.server.turbosched.TurboSchedManagerService;
import com.miui.whetstone.server.WhetstoneActivityManagerService;
import com.xiaomi.mirror.service.MirrorService;
import dalvik.system.PathClassLoader;
import java.util.HashSet;
import java.util.Set;
import miui.hardware.shoulderkey.ShoulderKeyManager;
import miui.mqsas.sdk.BootEventManager;
import miui.os.Build;
import miui.os.DeviceFeature;
@MiuiStubHead(manifestName = "com.android.server.MiuiStubImplManifest$$")
/* loaded from: classes.dex */
public class SystemServerImpl extends SystemServerStub {
    private static final boolean DEBUG = true;
    private static final String TAG = "SystemServerI";
    private static Set<String> sVersionPolicyDevices = new HashSet();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SystemServerImpl> {

        /* compiled from: SystemServerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SystemServerImpl INSTANCE = new SystemServerImpl();
        }

        public SystemServerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SystemServerImpl provideNewInstance() {
            return new SystemServerImpl();
        }
    }

    public SystemServerImpl() {
        if ("file".equals(SystemProperties.get("ro.crypto.type")) || "trigger_restart_framework".equals(SystemProperties.get("vold.decrypt"))) {
            enforceVersionPolicy();
        }
    }

    static {
        addDeviceName("cepheus");
        addDeviceName("onc");
        addDeviceName("onclite");
        addDeviceName("lavender");
        addDeviceName("grus");
        addDeviceName("violet");
        addDeviceName("davinci");
        addDeviceName("raphael");
        addDeviceName("davinciin");
        addDeviceName("raphaelin");
        addDeviceName("andromeda");
        addDeviceName("pavo");
        addDeviceName("crux");
        addDeviceName("pyxis");
        addDeviceName("vela");
        addDeviceName("begonia");
        addDeviceName("begoniain");
        addDeviceName("olive");
        addDeviceName("olivelite");
        addDeviceName("olivewood");
        addDeviceName("ginkgo");
        addDeviceName("willow");
        addDeviceName("tucana");
        addDeviceName("phoenix");
        addDeviceName("phoenixin");
        addDeviceName("picasso");
        addDeviceName("picassoin");
        PathClassLoader pathClassLoader = (PathClassLoader) SystemServerImpl.class.getClassLoader();
        pathClassLoader.addDexPath("/system/framework/miuix.jar");
        try {
            Log.i(TAG, "Load libmiui_service");
            System.loadLibrary("miui_service");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "can't loadLibrary libmiui_service", e);
        }
    }

    static void addDeviceName(String name) {
        sVersionPolicyDevices.add(name);
        sVersionPolicyDevices.add(name + "_ru");
        sVersionPolicyDevices.add(name + "_eea");
    }

    PhoneWindowManager createPhoneWindowManager() {
        return new MiuiPhoneWindowManager();
    }

    Class createLightsServices() {
        return MiuiLightsService.class;
    }

    final void addExtraServices(Context context, boolean onlyCore) {
        if (ShoulderKeyManager.SUPPORT_SHOULDERKEY || ShoulderKeyManager.SUPPORT_MIGAMEMACRO) {
            ServiceManager.addService("shoulderkey", new ShoulderKeyManagerService(context));
        }
        ServiceManager.addService("security", new SecurityManagerService(context, onlyCore));
        ServiceManager.addService("miuiwebview", new MiuiWebViewManagerService(context));
        ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).startService(MiuiInitServer.Lifecycle.class);
        SystemServiceManager ssm = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
        if (ssm != null) {
            ssm.startService(BackupManagerService.Lifecycle.class);
        }
        ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).startService(StepCounterManagerService.class);
        ServiceManager.addService(PerfShielderService.SERVICE_NAME, new PerfShielderService(context));
        GreezeManagerService.startService(context);
        MiuiCarServiceStub.get().publishCarService();
        try {
            ServiceManager.addService(TurboSchedManagerService.SERVICE_NAME, new TurboSchedManagerService(context));
        } catch (Exception e) {
            Slog.w(TAG, "Failed to start TurboSchedManagerService", e);
        }
        ServiceManager.addService("ProcessManager", new ProcessManagerService(context));
        ServiceManager.addService(SchedBoostService.SERVICE_NAME, new SchedBoostService(context));
        ServiceManager.addService(MiuiCldService.SERVICE_NAME, new MiuiCldService(context));
        ServiceManager.addService(MiuiFboService.SERVICE_NAME, MiuiFboService.getInstance().forSystemServerInitialization(context));
        ServiceManager.addService(MiuiMemoryService.SERVICE_NAME, new MiuiMemoryService(context));
        ServiceManager.addService(MiuiDfcService.SERVICE_NAME, MiuiDfcService.getInstance().forDfcInitialization(context));
        if (SystemProperties.getBoolean("persist.sys.debug.enable_sentinel_memory_monitor", false) && SystemProperties.getBoolean("ro.debuggable", false)) {
            ServiceManager.addService(MiuiSentinelService.SERVICE_NAME, new MiuiSentinelService(context));
        }
        ServiceManager.addService("whetstone.activity", new WhetstoneActivityManagerService(context));
        if (MiuiAppSizeCompatModeImpl.getInstance().isEnabled()) {
            ServiceManager.addService(MiuiSizeCompatService.SERVICE_NAME, new MiuiSizeCompatService(context));
        }
        try {
            ServiceManager.addService("MiuiInputManager", new MiuiInputManagerService(context));
        } catch (Exception e2) {
            Slog.i(TAG, "add MiuiInputManager error");
        }
        ScreenEffectService.startScreenEffectService();
        MiuiFgThread.initialMiuiFgThread();
        if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
            EnterpriseManagerService.init(context);
        }
        SmartPowerService.startService(context);
        MiGardService.startService(context);
        MirrorService.init(context);
        if (TextUtils.equals(SystemProperties.get("ro.miui.build.region", ""), "cn")) {
            ((SystemServiceManager) LocalServices.getService(SystemServiceManager.class)).startService(BrokenScreenRescueService.class);
        }
        if (ApplicationCompatUtilsStub.get().isAppCompatEnabled()) {
            ApplicationCompatRouterStub.get();
            if (ApplicationCompatUtilsStub.get().isContinuityEnabled()) {
                AppContinuityRouterStub.get();
            }
        }
    }

    void markSystemRun(long time) {
        long now = SystemClock.uptimeMillis();
        BootEventManager.getInstance().setZygotePreload(now - time);
        BootEventManager.getInstance().setSystemRun(time);
        if (MiuiEmbeddingWindowServiceStubHead.isActivityEmbeddingEnable()) {
            MiuiEmbeddingWindowServiceStubHead.get();
        }
        if (MiuiMagicPointerServiceStubHead.isMiuiMagicPointerEnable()) {
            MiuiMagicPointerServiceStubHead.get();
            MiuiMagicPointerUtilsStubHead.get();
        }
    }

    private static void rebootIntoRecovery() {
        BcbUtil.setupBcb("--show_version_mismatch\n");
        SystemProperties.set("sys.powerctl", "reboot,recovery");
    }

    private static boolean isGlobalHaredware(String product) {
        String country = SystemProperties.get("ro.boot.hwc");
        if (!"CN".equals(country)) {
            if (country != null && country.startsWith("CN_")) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static void enforceVersionPolicy() {
        String product = SystemProperties.get("ro.product.name");
        if (!sVersionPolicyDevices.contains(product)) {
            int api = SystemProperties.getInt("ro.product.first_api_level", 0);
            if (api < 29) {
                Slog.d(TAG, "enforceVersionPolicy: enable_flash_global enabled");
                return;
            }
        }
        if (!"locked".equals(SystemProperties.get("ro.secureboot.lockstate"))) {
            Slog.d(TAG, "enforceVersionPolicy: device unlocked");
        } else if (isGlobalHaredware(product)) {
            Slog.d(TAG, "enforceVersionPolicy: global device");
        } else if (Build.IS_INTERNATIONAL_BUILD) {
            Slog.e(TAG, "CN hardware can't run Global build; reboot into recovery!!!");
            rebootIntoRecovery();
        }
    }

    void markPmsScan(long startTime, long endTime) {
        BootEventManager.getInstance().setPmsScanStart(startTime);
        BootEventManager.getInstance().setPmsScanEnd(endTime);
    }

    void markBootDexopt(long startTime, long endTime) {
        BootEventManager.getInstance().setBootDexopt(endTime - startTime);
    }

    public String getMiuilibpath() {
        return ":/system_ext/framework/miui-wifi-service.jar";
    }

    void addMiuiRestoreManagerService(Context context, Installer installer) {
        try {
            Slog.d(TAG, "add MiuiRestoreManagerService");
            ServiceManager.addService(MiuiRestoreManagerService.SERVICE_NAME, new MiuiRestoreManagerService(context, installer));
        } catch (Throwable e) {
            Slog.d(TAG, "add MiuiRestoreManagerService fail " + e);
        }
    }

    public String getConnectivitylibpath() {
        return ":/system_ext/framework/miui-connectivity-service.jar";
    }

    void addCameraCoveredManagerService(Context context) {
        try {
            Slog.d(TAG, "add CameraCoveredManagerService");
            ServiceManager.addService(MiuiCameraCoveredManagerService.SERVICE_NAME, new MiuiCameraCoveredManagerService(context));
        } catch (Throwable e) {
            Slog.d(TAG, "add CameraCoveredManagerService fail " + e);
        }
    }

    void addMagicPointerManagerService(Context context, ActivityManagerService ams, WindowManagerService wms, InputManagerService inputManager) {
        if (DeviceFeature.IS_SUPPORT_MAGIC_POINTER) {
            MiuiMagicPointerServiceStub.get().publishMiuiMagicPointerService(context, ams, wms, inputManager);
        }
    }

    void magicPointerManagerServiceSystemReady() {
        if (MiuiMagicPointerServiceStub.get() != null) {
            try {
                MiuiMagicPointerServiceStub.get().systemReady();
            } catch (Throwable e) {
                Slog.d(TAG, "MiuiMagicPointerService.systemReady(); fail " + e);
            }
        }
    }
}
