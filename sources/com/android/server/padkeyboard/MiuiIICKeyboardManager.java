package com.android.server.padkeyboard;

import android.app.ActivityThread;
import android.app.ContextImpl;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.input.InputManagerService;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import com.android.server.padkeyboard.iic.IICNodeHelper;
import com.android.server.padkeyboard.iic.NanoSocketCallback;
import com.miui.server.input.PadManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import miui.hardware.input.MiuiKeyboardStatus;
import miui.util.FeatureParser;
import miuix.appcompat.R;
import miuix.appcompat.app.AlertDialog;
/* loaded from: classes.dex */
public class MiuiIICKeyboardManager implements NanoSocketCallback, MiuiPadKeyboardManager {
    private static final int CONSUMER_DEVICE_PRODUCT_ID = 164;
    private static final int DEVICE_PRODUCT_ID = 163;
    private static final int DEVICE_VENDOR_ID = 5593;
    private static final int MAX_CHECK_IDENTITY_TIMES = 5;
    private static final String PATH_KEYBOARD_BIN = "Keyboard_Upgrade.bin";
    private static final String PATH_MCU_BIN = "MCU_Upgrade.bin";
    public static final int WAKE_KEY_DEVICE_PRODUCT_ID = 148;
    public static final int WAKE_KEY_DEVICE_VENDOR_ID = 2087;
    private static volatile MiuiIICKeyboardManager sInstance;
    private final Sensor mAccSensor;
    private final AngleStateController mAngleStateController;
    private int mConsumerDeviceId;
    private final Context mContext;
    private AlertDialog mDialog;
    private final Handler mHandler;
    private final IICNodeHelper mIICNodeHelper;
    private boolean mIsKeyboardReady;
    private boolean mIsSetupComplete;
    private MiuiPadKeyboardManager.KeyboardAuthCallback mKeyboardAuthCallback;
    private int mKeyboardDeviceId;
    private String mKeyboardVersion;
    private String mMCUVersion;
    private boolean mScreenState;
    private final SensorManager mSensorManager;
    private boolean mShouldStayWakeKeyboard;
    private PowerManager.WakeLock mWakeLock;
    private final float[] mLocalGData = new float[3];
    private final float[] mKeyboardGData = new float[3];
    private final float G = 9.8f;
    private volatile boolean mCheckIdentityPass = false;
    private int mCheckIdentityTimes = 0;
    private boolean mPadAccReady = false;
    private boolean mKeyboardAccReady = false;
    private boolean mIsKeyboardSleep = true;
    private boolean mIsShouldInitKeyboard = true;
    private final InputManagerService mInputManager = ServiceManager.getService("input");

    public MiuiIICKeyboardManager(Context context) {
        Slog.i(MiuiPadKeyboardManager.TAG, "MiuiIICKeyboardManager");
        HandlerThread handlerThread = new HandlerThread("IIC_Pad_Manager");
        handlerThread.start();
        this.mContext = context;
        this.mHandler = new H(handlerThread.getLooper());
        MiuiKeyboardUtil.makeRemountDirFile(context);
        IICNodeHelper iICNodeHelper = IICNodeHelper.getInstance(context);
        this.mIICNodeHelper = iICNodeHelper;
        iICNodeHelper.setOtaCallBack(this);
        this.mAngleStateController = new AngleStateController(context);
        SensorManager sensorManager = (SensorManager) context.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mAccSensor = sensorManager.getDefaultSensor(1);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(6, "iic_upgrade");
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setMiuiKeyboardInfo(DEVICE_VENDOR_ID, DEVICE_PRODUCT_ID);
        inputCommonConfig.flushToNative();
    }

    public static MiuiIICKeyboardManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (MiuiIICKeyboardManager.class) {
                if (sInstance == null) {
                    sInstance = new MiuiIICKeyboardManager(context);
                }
            }
        }
        return sInstance;
    }

    public static boolean supportPadKeyboard() {
        return FeatureParser.getBoolean("support_iic_keyboard", false);
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (isKeyboardReady() && event.sensor.getType() == 1) {
            if (motionJudge(event.values[0], event.values[1], event.values[2])) {
                this.mPadAccReady = false;
            } else if (this.mIsKeyboardReady) {
                if (!this.mPadAccReady || Math.abs(event.values[0] - this.mLocalGData[0]) > 0.294f || Math.abs(event.values[1] - this.mLocalGData[1]) > 0.196f || Math.abs(event.values[2] - this.mLocalGData[2]) > 0.588f) {
                    float[] fArr = event.values;
                    float[] fArr2 = this.mLocalGData;
                    System.arraycopy(fArr, 0, fArr2, 0, fArr2.length);
                    this.mPadAccReady = true;
                    if (this.mIsKeyboardSleep) {
                        wakeKeyboard176();
                    }
                    calculateAngle();
                }
            }
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public InputDevice[] removeKeyboardDevicesIfNeeded(InputDevice[] allInputDevices) {
        ArrayList<InputDevice> tempDevices = new ArrayList<>();
        for (InputDevice device : allInputDevices) {
            if (device.getVendorId() == DEVICE_VENDOR_ID) {
                if (this.mIsKeyboardReady) {
                    return allInputDevices;
                }
                if (device.getProductId() == DEVICE_PRODUCT_ID) {
                    if (this.mKeyboardDeviceId != device.getId()) {
                        this.mKeyboardDeviceId = device.getId();
                    }
                } else if (device.getProductId() == CONSUMER_DEVICE_PRODUCT_ID) {
                    if (this.mConsumerDeviceId != device.getId()) {
                        this.mConsumerDeviceId = device.getId();
                    }
                }
            }
            tempDevices.add(device);
        }
        return (InputDevice[]) tempDevices.toArray(new InputDevice[0]);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void notifyLidSwitchChanged(boolean lidOpen) {
        Slog.i(MiuiPadKeyboardManager.TAG, "Lid Switch Changed to " + lidOpen);
        this.mAngleStateController.notifyLidSwitchChanged(lidOpen);
        enableOrDisableInputDevice();
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void notifyTabletSwitchChanged(boolean tabletOpen) {
        Slog.i(MiuiPadKeyboardManager.TAG, "TabletSwitch changed to " + tabletOpen);
        this.mAngleStateController.notifyTabletSwitchChanged(tabletOpen);
        enableOrDisableInputDevice();
        PadManager.getInstance().setIsTableOpen(tabletOpen);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onHallStatusChanged(byte status) {
        boolean tabletStatus = false;
        boolean lidStatus = (MiuiKeyboardUtil.byte2int(status) & 16) != 0;
        if ((MiuiKeyboardUtil.byte2int(status) & 1) != 0) {
            tabletStatus = true;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "Read Hall, lid:" + lidStatus + ", tablet:" + tabletStatus);
        notifyLidSwitchChanged(lidStatus);
        notifyTabletSwitchChanged(tabletStatus);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public int getKeyboardDeviceId() {
        return this.mKeyboardDeviceId;
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void notifyScreenState(boolean screenState) {
        if (this.mScreenState == screenState) {
            return;
        }
        this.mScreenState = screenState;
        if (!this.mIsShouldInitKeyboard && !isKeyboardReady()) {
            if (!screenState) {
                this.mSensorManager.unregisterListener(this);
                return;
            }
            return;
        }
        this.mIsShouldInitKeyboard = false;
        Slog.i(MiuiPadKeyboardManager.TAG, "Notify Screen State changed to " + screenState);
        if (this.mScreenState) {
            this.mAngleStateController.resetAngleStatus();
            enableOrDisableInputDevice();
            this.mSensorManager.registerListener(this, this.mAccSensor, 3);
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiIICKeyboardManager.this.m1215xff50308d();
                }
            }, 400L);
            return;
        }
        releaseWakeLock();
        stopWakeForKeyboard176();
        this.mIICNodeHelper.shutdownAllCommandQueue();
        this.mSensorManager.unregisterListener(this);
        this.mHandler.removeCallbacksAndMessages(null);
        this.mKeyboardAccReady = false;
        this.mPadAccReady = false;
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void getKeyboardReportData() {
        sendCommand(6);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void enableOrDisableInputDevice() {
        InputDevice inputDevice = this.mInputManager.getInputDevice(this.mKeyboardDeviceId);
        if (inputDevice != null && inputDevice.isEnabled() != this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            return;
        }
        if (this.mAngleStateController.shouldIgnoreKeyboardForIIC()) {
            Slog.i(MiuiPadKeyboardManager.TAG, "Disable Xiaomi Keyboard!");
            setCapsLockLight(false);
            this.mInputManager.disableInputDevice(this.mKeyboardDeviceId);
            this.mInputManager.disableInputDevice(this.mConsumerDeviceId);
        } else {
            Slog.i(MiuiPadKeyboardManager.TAG, "Enable Xiaomi Keyboard!");
            setCapsLockLight(PadManager.getInstance().getCapsLockStatus());
            this.mInputManager.enableInputDevice(this.mKeyboardDeviceId);
            this.mInputManager.enableInputDevice(this.mConsumerDeviceId);
        }
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public boolean isKeyboardReady() {
        return this.mIsKeyboardReady && this.mScreenState;
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onUpdateVersion(int type, String version) {
        String device = null;
        if (type == 20) {
            device = "Keyboard";
            this.mKeyboardVersion = version;
        } else if (type == 3) {
            device = "Flash Keyboard";
        } else if (type == 10) {
            device = "Pad MCU";
            this.mMCUVersion = version;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "getVersion for " + device + " is :" + version);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onOtaStateChange(int type, int status) {
        String target = type == 3 ? "Keyboard" : "MCU";
        if (status == 0) {
            acquireWakeLock();
            stayWakeForKeyboard176();
        }
        if (status == 2) {
            if (type == 3) {
                Context context = this.mContext;
                Toast.makeText(context, context.getResources().getString(286196276), 0).show();
            }
            Slog.i(MiuiPadKeyboardManager.TAG, target + " OTA Successfully!");
        }
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onWriteSocketErrorInfo(String reason) {
        Slog.e(MiuiPadKeyboardManager.TAG, "Write Socket Fail because:" + reason);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void readyToCheckAuth() {
        startCheckIdentity(0, true);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onOtaErrorInfo(int type, String reason) {
        if (type == 10) {
            Slog.e(MiuiPadKeyboardManager.TAG, "MCU OTA Fail because:" + reason);
        } else if (type == 20) {
            if (!NanoSocketCallback.OTA_ERROR_REASON_NO_VERSION.equals(reason)) {
                Context context = this.mContext;
                Toast.makeText(context, context.getResources().getString(286196274), 0).show();
            }
            Slog.e(MiuiPadKeyboardManager.TAG, "Keyboard OTA Fail because:" + reason);
        }
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onReadSocketNumError(String reason) {
        Slog.e(MiuiPadKeyboardManager.TAG, "read socket find num error:" + reason);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onOtaProgress(int type, float pro) {
        String target = type == 10 ? "MCU" : "Keyboard";
        if (pro == 50.0f || pro == 100.0f) {
            Slog.i(MiuiPadKeyboardManager.TAG, "Current upgrade progress： type = " + target + ", process: " + pro);
        }
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onAuthResult(byte[] data) {
        if (this.mKeyboardAuthCallback != null) {
            Slog.d(MiuiPadKeyboardManager.TAG, "keyboard authentication response:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
            if (data[4] == 49) {
                this.mKeyboardAuthCallback.respondKeyboard(data, 1);
            } else if (data[4] == 50) {
                this.mKeyboardAuthCallback.respondKeyboard(data, 2);
            }
        }
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardStateChanged(boolean isConnected) {
        if (this.mIsKeyboardReady == isConnected) {
            return;
        }
        Slog.i(MiuiPadKeyboardManager.TAG, "Keyboard link state changed:" + isConnected);
        boolean oldState = this.mIsKeyboardReady;
        this.mIsKeyboardReady = isConnected;
        if (isConnected) {
            this.mIICNodeHelper.setShouldUpgradeStatus();
            this.mSensorManager.registerListener(this, this.mAccSensor, 3);
        } else {
            this.mSensorManager.unregisterListener(this);
            this.mKeyboardAccReady = false;
            this.mPadAccReady = false;
        }
        if (!oldState) {
            m1215xff50308d();
        } else {
            onKeyboardDetach();
        }
        ReflectionUtils.callPrivateMethod(this.mInputManager, "notifyIgnoredInputDevicesChanged", new Object[0]);
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardGSensorChanged(float x, float y, float z) {
        float[] fArr = this.mKeyboardGData;
        fArr[0] = x;
        fArr[1] = y;
        fArr[2] = z;
        this.mKeyboardAccReady = true;
        calculateAngle();
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onKeyboardSleepStatusChanged(boolean isSleep) {
        if (this.mIsKeyboardSleep != isSleep) {
            this.mIsKeyboardSleep = isSleep;
            Slog.i(MiuiPadKeyboardManager.TAG, "Keyboard sleep status change to: " + this.mIsKeyboardSleep);
        }
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onKeyStateData(int state, int touchpad, int keyboard, int bacolight, int sensitivity, int x, int y, int z, int pen, int pen_battery, int power) {
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onKeyState(int state, int back) {
    }

    @Override // com.android.server.padkeyboard.iic.NanoSocketCallback
    public void onStateData(int state, byte k_state, String k_battry, int times) {
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void readKeyboardStatus() {
        sendCommand(9);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void readHallStatus() {
        sendCommand(12);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public byte[] sendCommandForRespond(byte[] command, MiuiPadKeyboardManager.CommandCallback callback) {
        byte[] result = this.mIICNodeHelper.checkAuth(command);
        if (result != null && (callback == null || callback.isCorrectPackage(result))) {
            return result;
        }
        return new byte[0];
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiDevAuthInit() {
        return MiuiKeyboardUtil.commandMiDevAuthInitForIIC();
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public byte[] commandMiAuthStep3Type1(byte[] keyMeta, byte[] challenge) {
        return MiuiKeyboardUtil.commandMiAuthStep3Type1ForIIC(keyMeta, challenge);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public MiuiKeyboardStatus getKeyboardStatus() {
        return new MiuiKeyboardStatus(this.mIsKeyboardReady, this.mAngleStateController.getIdentityStatus(), this.mAngleStateController.isWorkState(), this.mAngleStateController.getLidStatus(), this.mAngleStateController.getTabletStatus(), this.mAngleStateController.shouldIgnoreKeyboardForIIC(), this.mMCUVersion, this.mKeyboardVersion);
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void setCapsLockLight(boolean enable) {
        Bundle data = new Bundle();
        data.putInt(H.DATA_FEATURE_VALUE, enable ? 1 : 0);
        data.putInt(H.DATA_FEATURE_TYPE, CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getIndex());
        sendCommand(7, data);
    }

    public static boolean isXiaomiKeyboard(int vendorId, int productId) {
        return vendorId == DEVICE_VENDOR_ID && (productId == DEVICE_PRODUCT_ID || productId == CONSUMER_DEVICE_PRODUCT_ID);
    }

    public static int shouldClearActivityInfoFlags() {
        return 112;
    }

    @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager
    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println("MIuiI2CKeyboardManager");
        pw.print(prefix);
        pw.print("mMcuVersion=");
        pw.println(this.mMCUVersion);
        pw.print(prefix);
        pw.print("mKeyboardVersion=");
        pw.println(this.mKeyboardVersion);
        pw.print(prefix);
        pw.print("AuthState=");
        pw.println(this.mCheckIdentityPass);
        pw.print(prefix);
        pw.print("mIsKeyboardReady=");
        pw.println(this.mIsKeyboardReady);
        pw.print(prefix);
        pw.print("isKeyboardDisable=");
        pw.println(this.mAngleStateController.shouldIgnoreKeyboardForIIC());
        this.mAngleStateController.dump(prefix, pw);
        pw.print(prefix);
    }

    void sendCommand(int command) {
        sendCommand(command, new Bundle());
    }

    void sendCommand(int command, Bundle data) {
        sendCommand(command, data, 0);
    }

    void sendCommand(int command, Bundle data, int delay) {
        Message message = this.mHandler.obtainMessage();
        message.what = command;
        message.setData(data);
        this.mHandler.removeMessages(command);
        this.mHandler.sendMessageDelayed(message, delay);
    }

    public void writePadKeyBoardStatus(int target, int value) {
        Bundle data = new Bundle();
        data.putInt(H.DATA_FEATURE_VALUE, value);
        data.putInt(H.DATA_FEATURE_TYPE, target);
        sendCommand(7, data);
    }

    public boolean getAuthState() {
        return this.mCheckIdentityPass;
    }

    private void startCheckIdentity(int delay, boolean isFirst) {
        if (isKeyboardReady()) {
            acquireWakeLock();
            stayWakeForKeyboard176();
            if (isFirst) {
                this.mCheckIdentityTimes = 0;
            }
            Bundle bundle = new Bundle();
            bundle.putBoolean(H.DATA_KEY_FIRST_CHECK, isFirst);
            sendCommand(8, bundle, delay);
            return;
        }
        stopWakeForKeyboard176();
        releaseWakeLock();
        Slog.e(MiuiPadKeyboardManager.TAG, "skip check auth because screen off");
    }

    /* renamed from: onKeyboardAttach */
    public void m1215xff50308d() {
        this.mAngleStateController.wakeUpIfNeed(!this.mScreenState);
        this.mHandler.removeCallbacksAndMessages(null);
        setCapsLockLight(PadManager.getInstance().getCapsLockStatus());
        sendCommand(2);
        sendCommand(1);
        sendCommand(3);
    }

    private void onKeyboardDetach() {
        releaseWakeLock();
        stopWakeForKeyboard176();
        this.mIICNodeHelper.shutdownAllCommandQueue();
        this.mHandler.removeCallbacksAndMessages(null);
    }

    private boolean isUserSetUp() {
        if (!this.mIsSetupComplete) {
            boolean z = false;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0) {
                z = true;
            }
            this.mIsSetupComplete = z;
        }
        return this.mIsSetupComplete;
    }

    private void setAuthState(boolean state) {
        this.mCheckIdentityPass = state;
    }

    public void doCheckKeyboardIdentity(boolean isFirst) {
        int result = KeyboardAuthHelper.getInstance(this.mContext).checkKeyboardIdentity(this, isFirst);
        processIdentity(result);
        if (result != 2 || this.mCheckIdentityTimes == 4) {
            releaseWakeLock();
            stopWakeForKeyboard176();
        }
    }

    private void processIdentity(int identity) {
        switch (identity) {
            case 0:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth ok");
                this.mAngleStateController.setIdentityState(true);
                setAuthState(true);
                return;
            case 1:
                showRejectConfirmDialog(1);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity auth reject");
                this.mAngleStateController.setIdentityState(false);
                setAuthState(false);
                return;
            case 2:
                if (this.mCheckIdentityTimes < 5) {
                    startCheckIdentity(5000, false);
                    this.mCheckIdentityTimes++;
                }
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity need check again");
                this.mAngleStateController.setIdentityState(true);
                setAuthState(true);
                return;
            case 3:
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity internal error");
                this.mAngleStateController.setIdentityState(true);
                setAuthState(true);
                return;
            case 4:
                showRejectConfirmDialog(4);
                Slog.i(MiuiPadKeyboardManager.TAG, "keyboard identity transfer error");
                this.mAngleStateController.setIdentityState(false);
                setAuthState(false);
                return;
            default:
                return;
        }
    }

    private void showRejectConfirmDialog(int type) {
        String message;
        ContextImpl systemUiContext = ActivityThread.currentActivityThread().getSystemUiContext();
        switch (type) {
            case 1:
                message = systemUiContext.getResources().getString(286196272);
                break;
            case 4:
                message = systemUiContext.getResources().getString(286196273);
                break;
            default:
                return;
        }
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        AlertDialog create = new AlertDialog.Builder(systemUiContext, R.style.AlertDialog_Theme_DayNight).setCancelable(true).setMessage(message).setPositiveButton(systemUiContext.getResources().getString(286196271), (DialogInterface.OnClickListener) null).create();
        this.mDialog = create;
        WindowManager.LayoutParams attrs = create.getWindow().getAttributes();
        attrs.type = 2003;
        attrs.flags |= 131072;
        attrs.gravity = 17;
        attrs.privateFlags |= 272;
        this.mDialog.getWindow().setAttributes(attrs);
        this.mDialog.show();
    }

    private void calculateAngle() {
        if (this.mKeyboardAccReady && this.mPadAccReady) {
            float[] fArr = this.mKeyboardGData;
            float f = fArr[0];
            float f2 = fArr[1];
            float f3 = fArr[2];
            float[] fArr2 = this.mLocalGData;
            int resultAngle = MiuiKeyboardUtil.calculatePKAngleV2(f, f2, f3, fArr2[0], fArr2[1], fArr2[2]);
            if (resultAngle == -1) {
                Slog.i(MiuiPadKeyboardManager.TAG, "accel angle error > 0.98, dont update angle");
                return;
            }
            Slog.i(MiuiPadKeyboardManager.TAG, "result Angle = " + resultAngle);
            this.mAngleStateController.updateAngleState(resultAngle);
            enableOrDisableInputDevice();
            return;
        }
        Slog.e(MiuiPadKeyboardManager.TAG, "The Pad or Keyboard gsensor not ready, mKeyboardAccReady: " + this.mKeyboardAccReady + ", mPadAccReady: " + this.mPadAccReady);
    }

    private boolean motionJudge(float accX, float accY, float accZ) {
        float mCombAcc = (float) Math.sqrt((accX * accX) + (accY * accY) + (accZ * accZ));
        return Math.abs(mCombAcc - 9.8f) > 0.294f;
    }

    private void releaseWakeLock() {
        if (this.mWakeLock.isHeld()) {
            Slog.i(MiuiPadKeyboardManager.TAG, "stop wake lock for keyboard");
            this.mWakeLock.release();
        }
    }

    private void acquireWakeLock() {
        if (!this.mWakeLock.isHeld()) {
            Slog.i(MiuiPadKeyboardManager.TAG, "stay wake lock for keyboard");
            this.mWakeLock.acquire();
        }
    }

    public void wakeKeyboard176() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.m1217x20829857();
            }
        });
    }

    /* renamed from: lambda$wakeKeyboard176$1$com-android-server-padkeyboard-MiuiIICKeyboardManager */
    public /* synthetic */ void m1217x20829857() {
        this.mIICNodeHelper.setKeyboardFeature(true, CommunicationUtil.KB_FEATURE.KB_POWER.getIndex());
    }

    private void readKeyboardGSensor() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.padkeyboard.MiuiIICKeyboardManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiIICKeyboardManager.this.m1216x6c383e77();
            }
        });
    }

    /* renamed from: lambda$readKeyboardGSensor$2$com-android-server-padkeyboard-MiuiIICKeyboardManager */
    public /* synthetic */ void m1216x6c383e77() {
        this.mIICNodeHelper.setKeyboardFeature(true, CommunicationUtil.KB_FEATURE.KB_G_SENSOR.getIndex());
    }

    private void stayWakeForKeyboard176() {
        if (!this.mShouldStayWakeKeyboard) {
            Message msg = this.mHandler.obtainMessage(10);
            this.mHandler.sendMessageDelayed(msg, 10000L);
            this.mShouldStayWakeKeyboard = true;
        }
    }

    private void stopWakeForKeyboard176() {
        if (this.mShouldStayWakeKeyboard) {
            this.mShouldStayWakeKeyboard = false;
            this.mHandler.removeMessages(10);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final String DATA_FEATURE_TYPE = "feature";
        public static final String DATA_FEATURE_VALUE = "value";
        public static final String DATA_KEY_FIRST_CHECK = "is_first_check";
        public static final int MSG_CHECK_MCU_STATUS = 9;
        public static final int MSG_CHECK_STAY_WAKE_KEYBOARD = 10;
        public static final int MSG_GET_MCU_KB_VERSION = 4;
        public static final int MSG_READ_FORMAT_DATA = 11;
        public static final int MSG_READ_HALL_DATA = 12;
        public static final int MSG_READ_KB_STATUS = 6;
        public static final int MSG_RESTORE_MCU = 5;
        public static final int MSG_SET_KB_STATUS = 7;
        public static final int MSG_SET_KB_UPGRADE_FILE = 2;
        public static final int MSG_SET_MCU_UPGRADE_FILE = 1;
        public static final int MSG_START_CHECK_AUTH = 8;
        public static final int MSG_UPGRADE = 3;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        H(Looper looper) {
            super(looper);
            MiuiIICKeyboardManager.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.setUpgradeFile(1, "/vendor/etc/MCU_Upgrade.bin");
                    return;
                case 2:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.setUpgradeFile(3, "/vendor/etc/Keyboard_Upgrade.bin");
                    return;
                case 3:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.startUpgrade();
                    return;
                case 4:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendGetVersionCommand();
                    return;
                case 5:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.sendRestoreMcuCommand();
                    return;
                case 6:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        MiuiIICKeyboardManager.this.mIICNodeHelper.readKeyboardStatus();
                        return;
                    }
                    return;
                case 7:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        if (msg.getData().getInt(DATA_FEATURE_VALUE, 0) != 1) {
                            z = false;
                        }
                        boolean enable = z;
                        int feature = msg.getData().getInt(DATA_FEATURE_TYPE, -1);
                        MiuiIICKeyboardManager.this.mIICNodeHelper.setKeyboardFeature(enable, feature);
                        return;
                    }
                    return;
                case 8:
                    if (MiuiIICKeyboardManager.this.mIsKeyboardReady) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "start authentication");
                        Bundle bundle = msg.getData();
                        if (bundle != null) {
                            boolean isFirst = bundle.getBoolean(DATA_KEY_FIRST_CHECK, true);
                            MiuiIICKeyboardManager.this.doCheckKeyboardIdentity(isFirst);
                            return;
                        }
                        return;
                    }
                    return;
                case 9:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.checkMCUStatus();
                    return;
                case 10:
                    if (MiuiIICKeyboardManager.this.mShouldStayWakeKeyboard) {
                        MiuiIICKeyboardManager.this.wakeKeyboard176();
                        Message stayWakeMsg = MiuiIICKeyboardManager.this.mHandler.obtainMessage(10);
                        MiuiIICKeyboardManager.this.mHandler.sendMessageDelayed(stayWakeMsg, 10000L);
                        return;
                    }
                    return;
                case 11:
                default:
                    return;
                case 12:
                    MiuiIICKeyboardManager.this.mIICNodeHelper.checkHallStatus();
                    return;
            }
        }
    }
}
