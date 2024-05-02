package com.android.server.audio.dolbyeffect;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.IAudioService;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.audio.dolbyeffect.deviceinfo.BtDeviceInfo;
import com.android.server.audio.dolbyeffect.deviceinfo.DeviceInfoBase;
import com.android.server.audio.dolbyeffect.deviceinfo.UsbDeviceInfo;
import com.android.server.audio.dolbyeffect.deviceinfo.WiredDeviceInfo;
import com.android.server.pm.CloudControlPreinstallService;
import java.util.LinkedList;
import java.util.UUID;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class DolbyEffectController {
    private static final String EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String HEADSET_STATE = "state";
    static final int MANU_ID_XIAO_MI = 911;
    private static final int MSG_BT_STATE_CHANGED = 1;
    private static final int MSG_BT_STATE_CHANGED_DEVICEBROKER = 3;
    private static final int MSG_HEADSET_PLUG = 2;
    private static final int MSG_VOLUME_CHANGED = 0;
    private static final String TAG = "DolbyEffectController";
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static LinkedList<DeviceInfoBase> mCurrentDevices = new LinkedList<>();
    private static volatile DolbyEffectController sInstance;
    private static IAudioService sService;
    DolbyAudioEffectHelper dolbyAudioEffectHelper;
    private Context mContext;
    private HandlerThread mHandlerThread;
    DeviceChangeBroadcastReceiver mReceiver;
    private WorkHandler mWorkHandler;
    private int mVolumeThreshold = 10;
    private int mCurrentIndex = -1;

    private DolbyEffectController(Context context) {
        this.mContext = context;
    }

    public static DolbyEffectController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DolbyEffectController(context);
        }
        return sInstance;
    }

    public void init() {
        DolbyAudioEffectHelper dolbyAudioEffectHelper;
        try {
            try {
                try {
                    try {
                        Log.d(TAG, "DolbyEffectController init...");
                        this.mCurrentIndex = getService().getDeviceStreamVolume(3, 2);
                        this.mVolumeThreshold = (getService().getStreamMaxVolume(3) * 10) / 15;
                        int index = getService().getDeviceStreamVolume(3, 2);
                        String device_id = index > this.mVolumeThreshold ? DeviceId.SPK_VOLUME_HIGH : DeviceId.SPK_VOLUME_LOW;
                        DolbyAudioEffectHelper dolbyAudioEffectHelper2 = new DolbyAudioEffectHelper(0, 0);
                        this.dolbyAudioEffectHelper = dolbyAudioEffectHelper2;
                        if (dolbyAudioEffectHelper2.hasControl()) {
                            Log.d(TAG, "init: setSelectedTuningDevice hasControl, device_id: " + device_id);
                            this.dolbyAudioEffectHelper.setSelectedTuningDevice(0, device_id);
                        } else {
                            Log.d(TAG, "init: setSelectedTuningDevice do not hasControl");
                        }
                        HandlerThread handlerThread = new HandlerThread("DolbyEffect");
                        this.mHandlerThread = handlerThread;
                        handlerThread.start();
                        this.mWorkHandler = new WorkHandler(this.mHandlerThread.getLooper());
                        Log.d(TAG, "DolbyEffectController init done");
                        dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                        if (dolbyAudioEffectHelper == null) {
                            return;
                        }
                    } catch (UnsupportedOperationException e) {
                        Log.e(TAG, "init: UnsupportedOperationException" + e);
                        dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                        if (dolbyAudioEffectHelper == null) {
                            return;
                        }
                    }
                } catch (RemoteException e2) {
                    Log.e(TAG, "init: RemoteException " + e2);
                    dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                    if (dolbyAudioEffectHelper == null) {
                        return;
                    }
                }
            } catch (IllegalArgumentException e3) {
                Log.e(TAG, "init: IllegalArgumentException" + e3);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            } catch (RuntimeException e4) {
                Log.e(TAG, "init: RuntimeException" + e4);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            }
            dolbyAudioEffectHelper.release();
        } catch (Throwable th) {
            DolbyAudioEffectHelper dolbyAudioEffectHelper3 = this.dolbyAudioEffectHelper;
            if (dolbyAudioEffectHelper3 != null) {
                dolbyAudioEffectHelper3.release();
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    private class DeviceChangeBroadcastReceiver extends BroadcastReceiver {
        private DeviceChangeBroadcastReceiver() {
            DolbyEffectController.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                Log.d(DolbyEffectController.TAG, "onReceive: " + action);
                char c = 65535;
                switch (action.hashCode()) {
                    case -1676458352:
                        if (action.equals("android.intent.action.HEADSET_PLUG")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 1772843706:
                        if (action.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 2116862345:
                        if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                            c = 2;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        Message msg = Message.obtain();
                        msg.what = 2;
                        msg.obj = intent;
                        DolbyEffectController.this.mWorkHandler.sendMessage(msg);
                        return;
                    case 1:
                    case 2:
                        Message msg2 = Message.obtain();
                        msg2.what = 1;
                        msg2.obj = intent;
                        DolbyEffectController.this.mWorkHandler.sendMessage(msg2);
                        return;
                    default:
                        return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public WorkHandler(Looper looper) {
            super(looper);
            DolbyEffectController.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Log.d(DolbyEffectController.TAG, "receive MSG_VOLUME_CHANGED");
                    DolbyEffectController.this.onVolumeChanged((Intent) msg.obj);
                    return;
                case 1:
                    Log.d(DolbyEffectController.TAG, "receive MSG_BT_STATE_CHANGED");
                    DolbyEffectController.this.onBTStateChanged((Intent) msg.obj);
                    return;
                case 2:
                    Log.d(DolbyEffectController.TAG, "receive MSG_HEADSET_PLUG");
                    DolbyEffectController.this.onHeadsetPlug((Intent) msg.obj);
                    return;
                case 3:
                    Log.d(DolbyEffectController.TAG, "receive MSG_BT_STATE_CHANGED_DEVICEBROKER");
                    DolbyEffectController.this.onBTStateChangedFromDeviceBroker(msg.getData().getString(CloudControlPreinstallService.ConnectEntity.DEVICE), msg.getData().getString("profile"), msg.getData().getString(DolbyEffectController.HEADSET_STATE));
                    return;
                default:
                    return;
            }
        }
    }

    public void btStateChangedFromDeviceBroker(Bundle btinfo) {
        if (this.mWorkHandler != null) {
            Message msg = Message.obtain();
            msg.what = 3;
            msg.setData(btinfo);
            this.mWorkHandler.sendMessage(msg);
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    public void receiveVolumeChanged(Intent volumeChanged) {
        if (this.mWorkHandler != null) {
            int stream_type = volumeChanged.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
            if (stream_type == 3) {
                Message msg = Message.obtain();
                msg.what = 0;
                msg.obj = volumeChanged;
                this.mWorkHandler.sendMessage(msg);
                return;
            }
            return;
        }
        Log.e(TAG, "mWorkHandler doesn't init");
    }

    public void onHeadsetPlug(Intent intent) {
        boolean changed = false;
        try {
            int newDevices = getService().getDeviceMaskForStream(3);
            int state = intent.getIntExtra(HEADSET_STATE, 0);
            if (state == 0) {
                Log.d(TAG, "detected device disconnected, new devices: " + newDevices);
                if ((newDevices & BroadcastQueueImpl.FLAG_IMMUTABLE) == 0) {
                    changed = removeDeviceForName("USB headset");
                } else if ((newDevices & 12) == 0) {
                    changed = removeDeviceForName("wired headset");
                }
            } else if (state == 1) {
                Log.d(TAG, "detected device connected, new devices: " + newDevices);
                if ((67108864 & newDevices) != 0) {
                    UsbDeviceInfo newDevice = new UsbDeviceInfo(DeviceInfoBase.TYPE_USB, 0, 0);
                    if (containsDevice("USB headset") < 0) {
                        mCurrentDevices.add(newDevice);
                        changed = true;
                        Log.d(TAG, "onReceive: USB headset connected");
                    }
                } else if ((newDevices & 12) != 0) {
                    WiredDeviceInfo newDevice2 = new WiredDeviceInfo(DeviceInfoBase.TYPE_WIRED, 0, 0);
                    if (containsDevice("wired headset") < 0) {
                        mCurrentDevices.add(newDevice2);
                        changed = true;
                        Log.d(TAG, "onReceive: wired headset connected");
                    }
                }
            }
            if (changed) {
                onDeviceChanged(mCurrentDevices);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onHeadsetPlug: RemoteException " + e);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void onBTStateChanged(Intent intent) {
        boolean z;
        String action = intent.getAction();
        if (action != null) {
            boolean changed = false;
            switch (action.hashCode()) {
                case 1772843706:
                    if (action.equals("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 2116862345:
                    if (action.equals("android.bluetooth.device.action.BOND_STATE_CHANGED")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    int[] ids = new int[2];
                    String[] deviceName = {""};
                    if (BtDeviceInfo.tryGetIdsFromIntent(intent, ids, deviceName) && ids[0] != 0 && ids[1] != 0) {
                        int idx = containsDevice(deviceName[0]);
                        if (idx >= 0) {
                            BtDeviceInfo device2 = (BtDeviceInfo) mCurrentDevices.get(idx);
                            if (device2.getMajorID() != ids[0] && device2.getMinorID() != ids[1]) {
                                device2.setMajorID(ids[0]);
                                device2.setMinorID(ids[1]);
                                Log.d(TAG, "onBTStateChanged: device updated " + deviceName[0] + " majorid: " + ids[0] + " minorid: " + ids[1]);
                                changed = true;
                                break;
                            }
                        } else {
                            BtDeviceInfo newDevice = new BtDeviceInfo(DeviceInfoBase.TYPE_BT, ids[0], ids[1], deviceName[0], false);
                            mCurrentDevices.add(newDevice);
                            changed = true;
                            Log.d(TAG, "onBTStateChanged: device connected " + deviceName[0] + " majorid: " + ids[0] + " minorid: " + ids[1]);
                            break;
                        }
                    }
                    break;
                case true:
                    int state = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", -1);
                    BluetoothDevice extraDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    String deviceName2 = extraDevice != null ? extraDevice.getAddress() : "NULL";
                    if (state == 10) {
                        Log.d(TAG, "ACTION_BOND_STATE_CHANGED: device " + deviceName2 + " bond disconnect");
                        changed = removeDeviceForName(deviceName2);
                        break;
                    }
                    break;
            }
            if (changed) {
                onDeviceChanged(mCurrentDevices);
            }
        }
    }

    public void onBTStateChangedFromDeviceBroker(String device, String profile, String state) {
        int idx;
        Log.d(TAG, "onBTStateChangedFromDeviceBroker btDevice=" + device + " profile=" + profile + " state=" + state);
        if ("A2DP".equals(profile)) {
            if ("STATE_CONNECTED".equals(state)) {
                int idx2 = containsDevice(device);
                if (idx2 >= 0) {
                    BtDeviceInfo product = (BtDeviceInfo) mCurrentDevices.get(idx2);
                    mCurrentDevices.remove(idx2);
                    product.setState(true);
                    mCurrentDevices.add(product);
                    Log.d(TAG, "onBTStateChanged: device active: " + device + " majorid: " + product.getMajorID() + " minorid: " + product.getMinorID());
                } else {
                    mCurrentDevices.add(new BtDeviceInfo(DeviceInfoBase.TYPE_BT, 0, 0, device, true));
                }
                onDeviceChanged(mCurrentDevices);
            } else if ("STATE_DISCONNECTED".equals(state) && (idx = containsDevice(device)) >= 0) {
                ((BtDeviceInfo) mCurrentDevices.get(idx)).setState(false);
                onDeviceChanged(mCurrentDevices);
            }
        }
    }

    public void onVolumeChanged(Intent intent) {
        DolbyAudioEffectHelper dolbyAudioEffectHelper;
        int newLevel;
        int currentLevel;
        try {
            try {
                try {
                    try {
                        int newVolume = getService().getDeviceStreamVolume(3, 2);
                        int factor = (int) (this.mVolumeThreshold * 1.1d);
                        newLevel = newVolume / factor;
                        currentLevel = this.mCurrentIndex / factor;
                        this.mCurrentIndex = newVolume;
                    } catch (RemoteException e) {
                        Log.e(TAG, "onVolumeChanged: RemoteException " + e);
                        dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                        if (dolbyAudioEffectHelper == null) {
                            return;
                        }
                    }
                } catch (UnsupportedOperationException e2) {
                    Log.e(TAG, "onVolumeChanged: UnsupportedOperationException" + e2);
                    dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                    if (dolbyAudioEffectHelper == null) {
                        return;
                    }
                }
            } catch (IllegalArgumentException e3) {
                Log.e(TAG, "onVolumeChanged: IllegalArgumentException" + e3);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            } catch (RuntimeException e4) {
                Log.e(TAG, "onVolumeChanged: RuntimeException" + e4);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            }
            if (newLevel == currentLevel) {
                DolbyAudioEffectHelper dolbyAudioEffectHelper2 = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper2 == null) {
                    return;
                }
                dolbyAudioEffectHelper2.release();
                return;
            }
            String device_id = "";
            if (newLevel > currentLevel) {
                device_id = DeviceId.SPK_VOLUME_HIGH;
                Log.d(TAG, "volume from low to high");
            } else if (newLevel < currentLevel) {
                device_id = DeviceId.SPK_VOLUME_LOW;
                Log.d(TAG, "volume from high to low");
            }
            DolbyAudioEffectHelper dolbyAudioEffectHelper3 = new DolbyAudioEffectHelper(0, 0);
            this.dolbyAudioEffectHelper = dolbyAudioEffectHelper3;
            if (dolbyAudioEffectHelper3.hasControl()) {
                Log.d(TAG, "setSelectedTuningDevice hasControl, device_id = " + device_id);
                this.dolbyAudioEffectHelper.setSelectedTuningDevice(0, device_id);
            } else {
                Log.w(TAG, "setSelectedTuningDevice do not hasControl");
            }
            dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
            if (dolbyAudioEffectHelper == null) {
                return;
            }
            dolbyAudioEffectHelper.release();
        } catch (Throwable th) {
            DolbyAudioEffectHelper dolbyAudioEffectHelper4 = this.dolbyAudioEffectHelper;
            if (dolbyAudioEffectHelper4 != null) {
                dolbyAudioEffectHelper4.release();
            }
            throw th;
        }
    }

    private static IAudioService getService() {
        IAudioService iAudioService = sService;
        if (iAudioService != null) {
            return iAudioService;
        }
        IBinder b = ServiceManager.getService("audio");
        IAudioService asInterface = IAudioService.Stub.asInterface(b);
        sService = asInterface;
        return asInterface;
    }

    private boolean removeDeviceForType(int type) {
        boolean removed = false;
        for (int i = mCurrentDevices.size() - 1; i >= 0; i--) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDeviceType() == type) {
                Log.d(TAG, "remove device type: " + type);
                mCurrentDevices.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private int containsDevice(String deviceName) {
        for (int i = 0; i < mCurrentDevices.size(); i++) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDevice().equals(deviceName)) {
                return i;
            }
        }
        return -1;
    }

    private boolean removeDeviceForName(String deviceName) {
        for (int i = mCurrentDevices.size() - 1; i >= 0; i--) {
            DeviceInfoBase product = mCurrentDevices.get(i);
            if (product.getDevice().equals(deviceName)) {
                Log.d(TAG, "remove device name: " + deviceName);
                mCurrentDevices.remove(i);
                return true;
            }
        }
        return false;
    }

    private void onDeviceChanged(LinkedList<DeviceInfoBase> currentDevices) {
        DolbyAudioEffectHelper dolbyAudioEffectHelper;
        String device_id;
        String device_id2;
        try {
            try {
                try {
                    device_id = "";
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "onDeviceChanged: IllegalArgumentException" + e);
                    dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                    if (dolbyAudioEffectHelper == null) {
                        return;
                    }
                } catch (RuntimeException e2) {
                    Log.e(TAG, "onDeviceChanged: RuntimeException" + e2);
                    dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                    if (dolbyAudioEffectHelper == null) {
                        return;
                    }
                }
            } catch (RemoteException e3) {
                Log.e(TAG, "onDeviceChanged: RemoteException " + e3);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            } catch (UnsupportedOperationException e4) {
                Log.e(TAG, "onDeviceChanged: UnsupportedOperationException" + e4);
                dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper == null) {
                    return;
                }
            }
            if (currentDevices.isEmpty()) {
                int index = getService().getDeviceStreamVolume(3, 2);
                if (index > this.mVolumeThreshold) {
                    device_id2 = DeviceId.SPK_VOLUME_HIGH;
                    Log.d(TAG, "no devices connected and volume >= " + this.mVolumeThreshold + ",setSelectedTuningDevice to speaker_volume_high");
                } else {
                    device_id2 = DeviceId.SPK_VOLUME_LOW;
                    Log.d(TAG, "no devices connected and volume < " + this.mVolumeThreshold + ",setSelectedTuningDevice to speaker_volume_low");
                }
                DolbyAudioEffectHelper dolbyAudioEffectHelper2 = new DolbyAudioEffectHelper(0, 0);
                this.dolbyAudioEffectHelper = dolbyAudioEffectHelper2;
                if (dolbyAudioEffectHelper2.hasControl()) {
                    Log.d(TAG, "setSelectedTuningDevice hasControl, deviceId = " + device_id2);
                    this.dolbyAudioEffectHelper.setSelectedTuningDevice(0, device_id2);
                } else {
                    Log.d(TAG, "setSelectedTuningDevice do not hasControl");
                }
                DolbyAudioEffectHelper dolbyAudioEffectHelper3 = this.dolbyAudioEffectHelper;
                if (dolbyAudioEffectHelper3 == null) {
                    return;
                }
                dolbyAudioEffectHelper3.release();
                return;
            }
            int port = 0;
            DeviceInfoBase device = currentDevices.getLast();
            if (device.getDeviceType() == DeviceInfoBase.TYPE_USB) {
                UsbDeviceInfo device2 = (UsbDeviceInfo) device;
                port = 5;
                device_id = getDeviceidForDevice(DeviceInfoBase.TYPE_USB, device2.getVendorID(), device2.getProductID());
                Log.d(TAG, "setSelectedTuningDevice for USB devices");
            } else if (device.getDeviceType() == DeviceInfoBase.TYPE_WIRED) {
                WiredDeviceInfo device22 = (WiredDeviceInfo) device;
                port = 3;
                device_id = getDeviceidForDevice(DeviceInfoBase.TYPE_WIRED, device22.getVendorID(), device22.getProductID());
                Log.d(TAG, "setSelectedTuningDevice for wired devices");
            } else if (device.getDeviceType() == DeviceInfoBase.TYPE_BT) {
                BtDeviceInfo device23 = (BtDeviceInfo) device;
                int id1 = device23.getMajorID();
                int id2 = device23.getMinorID();
                boolean state = device23.getState();
                if (id1 <= 0 || id2 <= 0) {
                    Log.d(TAG, "don't meet conditions to setSelectedTuningDevice, majorId: " + id1 + ",minorid: " + id2 + ",state: " + state);
                    DolbyAudioEffectHelper dolbyAudioEffectHelper4 = this.dolbyAudioEffectHelper;
                    if (dolbyAudioEffectHelper4 == null) {
                        return;
                    }
                    dolbyAudioEffectHelper4.release();
                    return;
                }
                port = 4;
                if (state) {
                    device_id = getDeviceidForDevice(DeviceInfoBase.TYPE_BT, id1, id2);
                    Log.d(TAG, "setSelectedTuningDevice for bt devices, majorId: " + id1 + ",minorid: " + id2);
                } else {
                    device_id = DeviceId.BLUETOOTH_DEFAULT;
                    Log.d(TAG, "top bt device a2dp state is false, set deviceid to bt_default");
                }
            }
            DolbyAudioEffectHelper dolbyAudioEffectHelper5 = new DolbyAudioEffectHelper(0, 0);
            this.dolbyAudioEffectHelper = dolbyAudioEffectHelper5;
            if (dolbyAudioEffectHelper5.hasControl()) {
                Log.d(TAG, "setSelectedTuningDevice hasControl, deviceId = " + device_id);
                this.dolbyAudioEffectHelper.setSelectedTuningDevice(port, device_id);
            } else {
                Log.d(TAG, "setSelectedTuningDevice do not hasControl");
            }
            dolbyAudioEffectHelper = this.dolbyAudioEffectHelper;
            if (dolbyAudioEffectHelper == null) {
                return;
            }
            dolbyAudioEffectHelper.release();
        } catch (Throwable th) {
            DolbyAudioEffectHelper dolbyAudioEffectHelper6 = this.dolbyAudioEffectHelper;
            if (dolbyAudioEffectHelper6 != null) {
                dolbyAudioEffectHelper6.release();
            }
            throw th;
        }
    }

    private String getDeviceidForDevice(int type, int id1, int id2) {
        if (type == DeviceInfoBase.TYPE_USB) {
            String device_id = DeviceId.getUsbDeviceId(id1, id2);
            return device_id;
        } else if (type == DeviceInfoBase.TYPE_BT) {
            String device_id2 = DeviceId.getBtDeviceId(id1, id2);
            return device_id2;
        } else if (type != DeviceInfoBase.TYPE_WIRED) {
            return "";
        } else {
            String device_id3 = DeviceId.getWiredDeviceId(id1, id2);
            return device_id3;
        }
    }

    /* loaded from: classes.dex */
    public static class DolbyAudioEffectHelper extends AudioEffect {
        private static final int BLUETOOTH = 4;
        private static final int BYTES_PER_INT = 4;
        private static final int EFFECT_PARAM_SELECTED_TUNING = 4;
        private static final UUID EFFECT_TYPE_DOLBY_AUDIO_PROCESSING = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");
        private static final int HDMI = 1;
        private static final int HEADPHONE = 3;
        private static final int INTERNAL_SPEAKER = 0;
        private static final int MIRACAST = 2;
        private static final int USB = 5;

        public DolbyAudioEffectHelper(int priority, int audioSession) throws IllegalStateException, IllegalArgumentException, UnsupportedOperationException, RuntimeException {
            super(EFFECT_TYPE_NULL, EFFECT_TYPE_DOLBY_AUDIO_PROCESSING, priority, audioSession);
            if (audioSession == 0) {
                Log.i(DolbyEffectController.TAG, "Creating a DolbyAudioEffect to global output mix!");
            }
        }

        @Override // android.media.audiofx.AudioEffect
        public boolean hasControl() {
            try {
                return super.hasControl();
            } catch (IllegalStateException ie) {
                Log.e(DolbyEffectController.TAG, ie.toString());
                return false;
            }
        }

        private static int int32ToByteArray(int src, byte[] dst, int index) {
            int index2 = index + 1;
            dst[index] = (byte) (src & 255);
            int index3 = index2 + 1;
            dst[index2] = (byte) ((src >>> 8) & 255);
            dst[index3] = (byte) ((src >>> 16) & 255);
            dst[index3 + 1] = (byte) ((src >>> 24) & 255);
            return 4;
        }

        private void checkReturnValue(int ret) {
            if (ret < 0) {
                switch (ret) {
                    case EaseManager.EaseStyleDef.STOP /* -5 */:
                        throw new UnsupportedOperationException("DolbyAudioEffect: invalid parameter operation");
                    case EaseManager.EaseStyleDef.FRICTION /* -4 */:
                        throw new IllegalArgumentException("DolbyAudioEffect: bad parameter value");
                    default:
                        throw new RuntimeException("DolbyAudioEffect: set/get parameter error");
                }
            }
        }

        public void setSelectedTuningDevice(int port, String device) throws IllegalArgumentException {
            if (port < 0 || port > 5) {
                Log.e(DolbyEffectController.TAG, "ERROR in setSelectedTuningDevice(): Invalid port" + port);
                throw new IllegalArgumentException();
            }
            int devlen = device.length();
            byte[] baValue = new byte[devlen + 4];
            int index = 0 + int32ToByteArray(port, baValue, 0);
            System.arraycopy(device.getBytes(), 0, baValue, index, devlen);
            checkReturnValue(setParameter(4, baValue));
        }
    }
}
