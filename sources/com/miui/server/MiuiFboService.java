package com.miui.server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProviderUtils;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import miui.fbo.IFbo;
import miui.fbo.IFboManager;
import miui.hardware.CldManager;
/* loaded from: classes.dex */
public class MiuiFboService extends IFboManager.Stub {
    private static final int APP_GC_AND_DISCARD = 4;
    public static final int CHANGE_USB_STATUS = 7;
    private static final String CONNECT_NATIVESERVICE_NAME = "FboNativeService";
    public static final int CONNECT_NATIVE_SERVICE = 4;
    public static final String CONTINUE = "continue";
    private static final String FALSE = "false";
    private static final int FBO_STATECTL = 3;
    private static final String HAL_DEFAULT = "default";
    private static final String HAL_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String HAL_SERVICE_NAME = "vendor.xiaomi.hardware.fbo@1.0::IFbo";
    private static final String HANDLER_NAME = "fboServiceWork";
    private static final int IS_FBO_SUPPORTED = 1;
    private static final String MIUI_FBO_PROCESSED_DONE = "miui.intent.action.FBO_PROCESSED_DONE";
    public static final String MIUI_FBO_RECEIVER_START = "miui.intent.action.start";
    public static final String MIUI_FBO_RECEIVER_STARTAGAIN = "miui.intent.action.startAgain";
    public static final String MIUI_FBO_RECEIVER_STOP = "miui.intent.action.stop";
    public static final String MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER = "miui.intent.action.transferFboTrigger";
    private static final String NATIVE_SERVICE_KEY = "persist.sys.fboservice.ctrl";
    private static final String NATIVE_SOCKET_NAME = "fbs_native_socket";
    private static final int OVERLOAD_FBO_SUPPORTED = 5;
    public static final String SERVICE_NAME = "miui.fbo.service";
    public static final int START_FBO = 1;
    public static final int START_FBO_AGAIN = 2;
    public static final String STOP = "stop";
    public static final String STOPDUETOBATTERYTEMPERATURE = "stopDueTobatteryTemperature";
    public static final String STOPDUETOSCREEN = "stopDueToScreen";
    public static final int STOP_DUETO_BATTERYTEMPERATURE = 6;
    public static final int STOP_DUETO_SCREEN = 5;
    public static final int STOP_FBO = 3;
    private static final int TRIGGER_CLD = 2;
    private static final String TRUE = "true";
    private static CldManager cldManager;
    private static LocalSocket interactClientSocket;
    private static AlarmManager mAlarmManager;
    private static PendingIntent mPendingIntent;
    private static LocalServerSocket mServerSocket;
    private static String oneOfPackageNameList;
    private int batteryLevel;
    private int batteryStatus;
    private int batteryTemperature;
    private volatile boolean cldStrategyStatus;
    private Context mContext;
    private volatile IFbo mFboNativeService;
    private volatile FboServiceHandler mFboServiceHandler;
    private HandlerThread mFboServiceThread;
    private boolean screenOn;
    private String stagingData;
    private static final String TAG = MiuiFboService.class.getSimpleName();
    private static volatile MiuiFboService sInstance = null;
    private static boolean mKeepRunning = true;
    private static OutputStream outputStream = null;
    private static InputStream inputStream = null;
    private static ArrayList<String> packageNameList = new ArrayList<>();
    private static int listSize = 0;
    private static final Object mLock = new Object();
    private static Long numberOfCleanupMemory = 0L;
    private static ArrayList<String> packageName = new ArrayList<>();
    private static ArrayList<PendingIntent> pendingIntentList = new ArrayList<>();
    private int batteryStatusMark = 0;
    private boolean messageHasbeenSent = false;
    private boolean nativeIsRunning = false;
    private boolean globalSwitch = false;
    private boolean dueToScreenWait = false;
    private int screenOnTimes = 0;
    private boolean enableNightJudgment = true;
    private boolean usbState = true;

    /* loaded from: classes.dex */
    public final class FboServiceHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public FboServiceHandler(Looper looper) {
            super(looper, null);
            MiuiFboService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            IBinder binder;
            switch (msg.what) {
                case 1:
                    MiuiFboService.this.messageHasbeenSent = false;
                    Slog.d(MiuiFboService.TAG, "current screenOn" + MiuiFboService.this.screenOn + "current batteryStatus" + MiuiFboService.this.batteryStatus + "current batteryLevel" + MiuiFboService.this.batteryLevel + "current batteryTemperature" + MiuiFboService.this.batteryTemperature + "screenOnTimes" + MiuiFboService.this.screenOnTimes);
                    if (MiuiFboService.this.screenOnTimes == 0 && !MiuiFboService.this.screenOn && MiuiFboService.this.batteryStatus > 0 && MiuiFboService.this.batteryLevel > 75 && MiuiFboService.this.batteryTemperature < 350) {
                        try {
                            if (MiuiFboService.listSize >= 0) {
                                MiuiFboService.oneOfPackageNameList = (String) MiuiFboService.packageNameList.remove(MiuiFboService.listSize);
                                MiuiFboService.startAppGcAndDiscard(MiuiFboService.oneOfPackageNameList);
                                MiuiFboService.this.mFboNativeService.FBO_trigger(MiuiFboService.oneOfPackageNameList);
                                MiuiFboService.this.setNativeIsRunning(true);
                                MiuiFboService.this.setGlobalSwitch(true);
                            }
                            MiuiFboService.listSize--;
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    } else if (MiuiFboService.this.batteryStatus <= 0 || MiuiFboService.this.batteryLevel < 70 || MiuiFboService.this.batteryTemperature >= 500) {
                        Slog.d(MiuiFboService.TAG, "do not meet the conditions exit");
                        return;
                    } else {
                        MiuiFboService unused = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_START, null, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        return;
                    }
                case 2:
                    Slog.d(MiuiFboService.TAG, "current screenOn" + MiuiFboService.this.screenOn + "current batteryStatus" + MiuiFboService.this.batteryStatus + "current batteryLevel" + MiuiFboService.this.batteryLevel + "current batteryTemperature" + MiuiFboService.this.batteryTemperature + "screenOnTimes" + MiuiFboService.this.screenOnTimes);
                    if (MiuiFboService.this.screenOnTimes == 0 && !MiuiFboService.this.screenOn && MiuiFboService.this.batteryStatus > 0 && MiuiFboService.this.batteryLevel > 75 && MiuiFboService.this.batteryTemperature < 350) {
                        try {
                            MiuiFboService.useCldStrategy(1);
                            MiuiFboService.this.mFboNativeService.FBO_stateCtl("continue," + MiuiFboService.this.stagingData);
                            MiuiFboService.this.mFboNativeService.FBO_trigger(MiuiFboService.oneOfPackageNameList);
                            MiuiFboService.this.sendStopOrContinueToHal(MiuiFboService.CONTINUE);
                            MiuiFboService.this.setNativeIsRunning(true);
                            MiuiFboService.this.setDueToScreenWait(false);
                            return;
                        } catch (Exception e2) {
                            Slog.d(MiuiFboService.TAG, "fail to execute START_FBO_AGAIN");
                            return;
                        }
                    } else if (MiuiFboService.this.batteryStatus <= 0 || MiuiFboService.this.batteryLevel < 70 || MiuiFboService.this.batteryTemperature >= 500) {
                        Slog.d(MiuiFboService.TAG, "do not meet the conditions exit");
                        return;
                    } else {
                        MiuiFboService unused2 = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN, null, 600000L, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        return;
                    }
                case 3:
                    try {
                        MiuiFboService.useCldStrategy(0);
                        MiuiFboService.this.mFboNativeService.FBO_stateCtl(MiuiFboService.STOP);
                        MiuiFboService.this.sendStopOrContinueToHal(MiuiFboService.STOP);
                        MiuiFboService.this.setNativeIsRunning(false);
                        MiuiFboService.this.setGlobalSwitch(false);
                        Slog.d(MiuiFboService.TAG, "All stop,exit");
                        MiuiFboService.clearBroadcastData();
                        SystemProperties.set(MiuiFboService.NATIVE_SERVICE_KEY, MiuiFboService.FALSE);
                        MiuiFboService.this.mFboServiceHandler.removeCallbacksAndMessages(null);
                        return;
                    } catch (Exception e3) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_FBO");
                        return;
                    }
                case 4:
                    try {
                        binder = ServiceManager.getService(MiuiFboService.CONNECT_NATIVESERVICE_NAME);
                        if (binder != null) {
                            binder.linkToDeath(new IBinder.DeathRecipient() { // from class: com.miui.server.MiuiFboService.FboServiceHandler.1
                                @Override // android.os.IBinder.DeathRecipient
                                public void binderDied() {
                                    Slog.w(MiuiFboService.TAG, "FboNativeService died; reconnecting");
                                    MiuiFboService.this.mFboNativeService = null;
                                }
                            }, 0);
                        }
                    } catch (Exception e4) {
                        binder = null;
                        SystemProperties.set(MiuiFboService.NATIVE_SERVICE_KEY, MiuiFboService.FALSE);
                    }
                    if (binder != null) {
                        MiuiFboService.this.mFboNativeService = IFbo.Stub.asInterface(binder);
                    } else {
                        MiuiFboService.this.mFboNativeService = null;
                        Slog.w(MiuiFboService.TAG, "IFbo not found; trying again");
                    }
                    if (MiuiFboService.this.mFboNativeService != null) {
                        MiuiFboService.sInstance.FBO_trigger(msg.obj.toString());
                        return;
                    }
                    return;
                case 5:
                    try {
                        MiuiFboService.useCldStrategy(0);
                        MiuiFboService miuiFboService = MiuiFboService.this;
                        miuiFboService.stagingData = miuiFboService.mFboNativeService.FBO_stateCtl(MiuiFboService.STOP);
                        Slog.d(MiuiFboService.TAG, "stopDueToScreen && sreenStagingData:" + MiuiFboService.this.stagingData);
                        MiuiFboService.this.sendStopOrContinueToHal(MiuiFboService.STOP);
                        MiuiFboService.this.setNativeIsRunning(false);
                        MiuiFboService.this.setDueToScreenWait(true);
                        MiuiFboService unused3 = MiuiFboService.sInstance;
                        MiuiFboService.setAlarm(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN, null, 600000L, false);
                        MiuiFboService.this.screenOnTimes = 0;
                        return;
                    } catch (Exception e5) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_DUETO_SCREEN");
                        return;
                    }
                case 6:
                    try {
                        MiuiFboService.useCldStrategy(0);
                        MiuiFboService miuiFboService2 = MiuiFboService.this;
                        miuiFboService2.stagingData = miuiFboService2.mFboNativeService.FBO_stateCtl(MiuiFboService.STOP);
                        Slog.d(MiuiFboService.TAG, "stopDueTobatteryTemperature && batteryTemperatureStagingData:" + MiuiFboService.this.stagingData);
                        MiuiFboService.this.sendStopOrContinueToHal(MiuiFboService.STOP);
                        MiuiFboService.this.setNativeIsRunning(false);
                        return;
                    } catch (Exception e6) {
                        Slog.d(MiuiFboService.TAG, "fail to execute STOP_DUETO_BATTERYTEMPERATURE");
                        return;
                    }
                case 7:
                    try {
                        String state = FileUtils.readTextFile(new File("/sys/class/android_usb/android0/state"), 128, "");
                        MiuiFboService.this.setUsbState("CONFIGURED".equals(state.trim()));
                        Slog.d(MiuiFboService.TAG, "USBState:" + MiuiFboService.this.getUsbState());
                        return;
                    } catch (Exception e7) {
                        Slog.e(MiuiFboService.TAG, "Failed to determine if device was on USB", e7);
                        MiuiFboService.this.setUsbState(true);
                        return;
                    }
                default:
                    Slog.d(MiuiFboService.TAG, "Unrecognized message command");
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class AlarmReceiver extends BroadcastReceiver {
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            Slog.d(MiuiFboService.TAG, "received the broadcast and intent.action : " + intent.getAction() + ",intent.getStringExtra:" + intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1989312766:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_STARTAGAIN)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -467317708:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1208808190:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_START)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case 1285920230:
                    if (action.equals(MiuiFboService.MIUI_FBO_RECEIVER_STOP)) {
                        c = 3;
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
                    if (MiuiFboService.sInstance.isWithinTheTimeInterval() && !MiuiFboService.sInstance.getGlobalSwitch()) {
                        MiuiFboService unused = MiuiFboService.sInstance;
                        MiuiFboService.sendMessage(MiuiFboService.packageNameList.toString(), 1, 1000L);
                        Slog.d(MiuiFboService.TAG, "carry out START_FBO");
                        return;
                    }
                    return;
                case 1:
                    MiuiFboService unused2 = MiuiFboService.sInstance;
                    MiuiFboService.sendMessage(MiuiFboService.packageNameList.toString(), 2, 1000L);
                    Slog.d(MiuiFboService.TAG, "carry out START_FBO_AGAIN");
                    return;
                case 2:
                    if (intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME) != null && intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME).length() > 0) {
                        MiuiFboService.sInstance.FBO_trigger(intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
                        Slog.d(MiuiFboService.TAG, "carry out TRANSFERFBOTRIGGER and appList:" + intent.getStringExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME));
                        return;
                    }
                    return;
                case 3:
                    if (MiuiFboService.sInstance.getGlobalSwitch() || MiuiFboService.sInstance.mFboNativeService != null) {
                        MiuiFboService unused3 = MiuiFboService.sInstance;
                        MiuiFboService.sendMessage(MiuiFboService.STOP, 3, 1000L);
                        Slog.d(MiuiFboService.TAG, "carry out STOP");
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiFboService forSystemServerInitialization(Context context) {
        SystemProperties.set(NATIVE_SERVICE_KEY, FALSE);
        this.mContext = context;
        cldManager = CldManager.getInstance(context);
        mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        IntentFilter intentFilter = new IntentFilter();
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        intentFilter.addAction(MIUI_FBO_RECEIVER_START);
        intentFilter.addAction(MIUI_FBO_RECEIVER_STARTAGAIN);
        intentFilter.addAction(MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER);
        intentFilter.addAction(MIUI_FBO_RECEIVER_STOP);
        this.mContext.registerReceiver(alarmReceiver, intentFilter);
        return getInstance();
    }

    public static MiuiFboService getInstance() {
        if (sInstance == null) {
            synchronized (MiuiFboService.class) {
                if (sInstance == null) {
                    sInstance = new MiuiFboService();
                }
            }
        }
        return sInstance;
    }

    private MiuiFboService() {
        initFboSocket();
        HandlerThread handlerThread = new HandlerThread(HANDLER_NAME);
        this.mFboServiceThread = handlerThread;
        handlerThread.start();
        this.mFboServiceHandler = new FboServiceHandler(this.mFboServiceThread.getLooper());
    }

    private void connect(String parameter) {
        SystemProperties.set(NATIVE_SERVICE_KEY, TRUE);
        sendMessage(parameter, 4, ActivityManagerServiceImpl.BOOST_DURATION);
    }

    private static void initFboSocket() {
        Thread thread = new Thread(new Runnable() { // from class: com.miui.server.MiuiFboService.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    MiuiFboService.mServerSocket = new LocalServerSocket(MiuiFboService.NATIVE_SOCKET_NAME);
                } catch (IOException e) {
                    e.printStackTrace();
                    MiuiFboService.mKeepRunning = false;
                }
                while (MiuiFboService.mKeepRunning) {
                    HwParcel hidl_reply = new HwParcel();
                    try {
                        try {
                            MiuiFboService.interactClientSocket = MiuiFboService.mServerSocket.accept();
                            MiuiFboService.inputStream = MiuiFboService.interactClientSocket.getInputStream();
                            MiuiFboService.outputStream = MiuiFboService.interactClientSocket.getOutputStream();
                            byte[] bytes = new byte[102400];
                            MiuiFboService.inputStream.read(bytes);
                            String dataReceived = new String(bytes);
                            String dataReceived2 = dataReceived.substring(0, dataReceived.indexOf(125));
                            Slog.d(MiuiFboService.TAG, "Receive data from native:" + dataReceived2);
                            if (dataReceived2.contains("pkg:")) {
                                MiuiFboService.sInstance.cldStrategyStatus = true;
                                MiuiFboService.useCldStrategy(1);
                                MiuiFboService.sInstance.cldStrategyStatus = false;
                                MiuiFboService.aggregateBroadcastData(dataReceived2);
                                MiuiFboService.sendMessage(MiuiFboService.packageNameList.toString(), 1, 0L);
                                if (MiuiFboService.listSize < 0) {
                                    MiuiFboService.sendMessage(MiuiFboService.STOP, 3, 0L);
                                }
                                hidl_reply.release();
                                try {
                                    MiuiFboService.inputStream.close();
                                    MiuiFboService.outputStream.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            } else {
                                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", MiuiFboService.HAL_DEFAULT);
                                if (hwService != null) {
                                    HwParcel hidl_request = new HwParcel();
                                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                                    hidl_request.writeString(dataReceived2);
                                    hwService.transact(2, hidl_request, hidl_reply, 0);
                                    hidl_reply.verifySuccess();
                                    hidl_request.releaseTemporaryStorage();
                                    String halReturnData = hidl_reply.readString();
                                    String[] split = halReturnData.split(":");
                                    Long beforeCleanup = Long.valueOf(Long.parseLong(split[split.length - 2]));
                                    Long afterCleanup = Long.valueOf(Long.parseLong(split[split.length - 1]));
                                    MiuiFboService.numberOfCleanupMemory = Long.valueOf(MiuiFboService.numberOfCleanupMemory.longValue() + (beforeCleanup.longValue() - afterCleanup.longValue()));
                                }
                                MiuiFboService.outputStream.write("{send message to native}".getBytes());
                                hidl_reply.release();
                                try {
                                    MiuiFboService.inputStream.close();
                                    MiuiFboService.outputStream.close();
                                } catch (IOException e3) {
                                    e3.printStackTrace();
                                }
                            }
                        } catch (Throwable e4) {
                            hidl_reply.release();
                            try {
                                MiuiFboService.inputStream.close();
                                MiuiFboService.outputStream.close();
                            } catch (IOException e5) {
                                e5.printStackTrace();
                            }
                            throw e4;
                        }
                    } catch (Exception e6) {
                        e6.printStackTrace();
                        MiuiFboService.writeFailToHal();
                        hidl_reply.release();
                        MiuiFboService.inputStream.close();
                        MiuiFboService.outputStream.close();
                    }
                }
            }
        });
        thread.start();
    }

    public static void aggregateBroadcastData(String dataReceived) {
        try {
            synchronized (mLock) {
                String[] splitDataReceived = dataReceived.split(":");
                packageName.add(splitDataReceived[1]);
                Long beforeCleaning = Long.valueOf(Long.parseLong(splitDataReceived[3]));
                Long afterCleaning = Long.valueOf(Long.parseLong(splitDataReceived[4]));
                numberOfCleanupMemory = Long.valueOf(numberOfCleanupMemory.longValue() + (beforeCleaning.longValue() - afterCleaning.longValue()));
                sInstance.reportFboProcessedBroadcast();
                outputStream.write("{broadcast send success}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeFailToNative();
        }
    }

    public static void writeFailToHal() {
        try {
            OutputStream outputStream2 = outputStream;
            if (outputStream2 != null) {
                outputStream2.write("{fail}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeFailToNative() {
        try {
            OutputStream outputStream2 = outputStream;
            if (outputStream2 != null) {
                outputStream2.write("{broadcast send fail}".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reportFboProcessedBroadcast() {
        Intent intent = new Intent(MIUI_FBO_PROCESSED_DONE);
        intent.putExtra("resultNumber", numberOfCleanupMemory);
        intent.putStringArrayListExtra("resultPkg", packageName);
        this.mContext.sendBroadcast(intent);
    }

    public boolean FBO_isSupport() {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                    hwService.transact(1, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    boolean flag = hidl_reply.readBool();
                    Slog.d(TAG, "return hidl_reply.readBool();" + flag);
                    return flag;
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to user MiuiFboService FBO_isSupport:" + e);
            }
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    public boolean FBO_new_isSupport(String val) {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                    hidl_request.writeString(val);
                    hwService.transact(5, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    boolean flag = hidl_reply.readBool();
                    return flag;
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to user MiuiFboService FBO_isSupport(String val):" + e);
            }
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    public void FBO_new_trigger(String appList, boolean flag) {
        if (flag) {
            setEnableNightJudgment(false);
        } else {
            setEnableNightJudgment(true);
        }
        sInstance.FBO_trigger(appList);
    }

    public void FBO_trigger(String appList) {
        if (getGlobalSwitch() || this.messageHasbeenSent) {
            Slog.d(TAG, "Currently executing fbo or waiting for execution message, so store application list");
            suspendTransferFboTrigger(appList, false, true);
        } else if (!isWithinTheTimeInterval() && !getGlobalSwitch()) {
            Slog.d(TAG, "transfer suspendTransferFboTrigger()");
            suspendTransferFboTrigger(appList, true, false);
        } else if (getUsbState()) {
            Slog.d(TAG, "will not execute fbo service because usbState");
        } else {
            String str = TAG;
            Slog.d(str, "MiuiFboService FBO_trigger success and appList is:" + appList);
            if (this.mFboNativeService == null) {
                connect(appList);
                return;
            }
            try {
                if (this.mFboNativeService != null && isWithinTheTimeInterval() && !getGlobalSwitch()) {
                    removePendingIntentList();
                    formatAppList(appList);
                    listSize = packageNameList.size() - 1;
                    Slog.i(str, "appList is:" + packageNameList + "listSize:" + listSize);
                    setAlarm(MIUI_FBO_RECEIVER_START, null, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION, false);
                    sendStopMessage();
                    this.messageHasbeenSent = true;
                    this.screenOnTimes = 0;
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to user MiuiFboService FBO_trigger:" + e);
            }
        }
    }

    public void FBO_notifyFragStatus() {
        try {
            reportFboProcessedBroadcast();
        } catch (Exception e) {
            Slog.e(TAG, "fail to user reportFboProcessedBroadcast():" + e);
        }
    }

    private static void formatAppList(String appList) {
        packageNameList.clear();
        String[] split = appList.split(",");
        for (String str : split) {
            String[] splitFinal = str.split("\"");
            packageNameList.add(splitFinal[1]);
        }
    }

    public static void setAlarm(String action, String appList, long delayTime, boolean record) {
        Intent intent = new Intent();
        intent.setAction(action);
        if (appList != null) {
            intent.putExtra(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME, appList);
        }
        PendingIntent broadcast = PendingIntent.getBroadcast(sInstance.mContext, 0, intent, BroadcastQueueImpl.FLAG_IMMUTABLE);
        mPendingIntent = broadcast;
        if (record) {
            pendingIntentList.add(broadcast);
        }
        mAlarmManager.setExactAndAllowWhileIdle(2, SystemClock.elapsedRealtime() + delayTime, mPendingIntent);
    }

    private static void suspendTransferFboTrigger(String appList, boolean cancel, boolean record) {
        PendingIntent pendingIntent;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int minuteOfDay = (hour * 60) + minute;
        int startTime = 1440 - minuteOfDay;
        if (cancel && (pendingIntent = mPendingIntent) != null) {
            mAlarmManager.cancel(pendingIntent);
        }
        removePendingIntentList();
        if (record) {
            setAlarm(MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER, appList, startTime * 60 * 1000, true);
        } else {
            setAlarm(MIUI_FBO_RECEIVER_TRANSFERFBOTRIGGER, appList, startTime * 60 * 1000, false);
        }
        Slog.d(TAG, "suspendTransferFboTrigger and suspendTime:" + startTime);
    }

    private static void removePendingIntentList() {
        if (pendingIntentList.size() > 0) {
            for (int i = pendingIntentList.size() - 1; i >= 0; i--) {
                PendingIntent PendingIntent = pendingIntentList.remove(i);
                mAlarmManager.cancel(PendingIntent);
            }
            Slog.d(TAG, "removePendingIntentList");
        }
    }

    public boolean isWithinTheTimeInterval() {
        if (!getEnableNightJudgment()) {
            return true;
        }
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int minuteOfDay = (hour * 60) + minute;
        if (minuteOfDay >= 0 && minuteOfDay <= 300) {
            Slog.d(TAG, "until five in the morning :" + (MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY - minuteOfDay));
            return true;
        }
        return false;
    }

    private void sendStopMessage() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int stopTime = 300 - ((hour * 60) + minute);
        setAlarm(MIUI_FBO_RECEIVER_STOP, null, stopTime * 60 * 1000, false);
        Slog.d(TAG, "stop time :" + stopTime);
    }

    public static void startAppGcAndDiscard(String appName) {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                    hidl_request.writeString(appName);
                    hwService.transact(4, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    Slog.d(TAG, "startAppGc:" + appName);
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to use startAppGc:" + e);
            }
        } finally {
            hidl_reply.release();
        }
    }

    public void sendStopOrContinueToHal(String cmd) {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.fbo@1.0::IFbo", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fbo@1.0::IFbo");
                    hidl_request.writeString(cmd);
                    hwService.transact(3, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    Slog.d(TAG, "sendStopOrContinueToHal:" + cmd);
                }
            } catch (Exception e) {
                Slog.e(TAG, "fail to user sendStopOrContinueToHal:" + e);
            }
        } finally {
            hidl_reply.release();
        }
    }

    public static void useCldStrategy(int val) {
        try {
            if (cldManager.isCldSupported() && sInstance.cldStrategyStatus) {
                cldManager.triggerCld(val);
            }
        } catch (Exception e) {
            Slog.e(TAG, "fail to use useCldStrategy:" + e);
        }
    }

    public static void clearBroadcastData() {
        numberOfCleanupMemory = 0L;
        packageName.clear();
    }

    public static void sendMessage(String data, int number, long sleepTime) {
        Message message = sInstance.mFboServiceHandler.obtainMessage();
        message.what = number;
        message.obj = data;
        sInstance.mFboServiceHandler.sendMessageDelayed(message, sleepTime);
        Slog.d(TAG, "msg.what = " + message.what + "send message time = " + System.currentTimeMillis());
    }

    public void setBatteryInfos(int batteryStatus, int batteryLevel, int batteryTemperature) {
        if (batteryStatus != this.batteryStatusMark) {
            this.batteryStatusMark = batteryStatus;
            sendMessage("", 7, 10000L);
        }
        setBatteryStatus(batteryStatus);
        setBatteryLevel(batteryLevel);
        setBatteryTemperature(batteryTemperature);
    }

    public void setScreenStatus(boolean screenOn) {
        this.screenOnTimes++;
        this.screenOn = screenOn;
    }

    public void setBatteryStatus(int batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setBatteryTemperature(int batteryTemperature) {
        this.batteryTemperature = batteryTemperature;
    }

    public void setNativeIsRunning(boolean nativeIsRunning) {
        this.nativeIsRunning = nativeIsRunning;
    }

    public boolean getNativeIsRunning() {
        return this.nativeIsRunning;
    }

    public void setGlobalSwitch(boolean globalSwitch) {
        this.globalSwitch = globalSwitch;
    }

    public boolean getGlobalSwitch() {
        return this.globalSwitch;
    }

    public void setDueToScreenWait(boolean dueToScreenWait) {
        this.dueToScreenWait = dueToScreenWait;
    }

    public boolean getDueToScreenWait() {
        return this.dueToScreenWait;
    }

    public void setEnableNightJudgment(boolean enableNightJudgment) {
        this.enableNightJudgment = enableNightJudgment;
    }

    public boolean getEnableNightJudgment() {
        return this.enableNightJudgment;
    }

    public void setUsbState(boolean usbState) {
        this.usbState = usbState;
    }

    public boolean getUsbState() {
        return this.usbState;
    }
}
