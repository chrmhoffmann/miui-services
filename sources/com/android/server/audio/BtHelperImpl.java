package com.android.server.audio;

import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.media.AudioSystem;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import java.util.Objects;
/* loaded from: classes.dex */
public class BtHelperImpl implements BtHelperStub {
    private static final int AUDIO_FORMAT_FORCE_AOSP = 2130706432;
    private static final int SPATIAL_AUDIO_TYPE_SUPPORT_GYRO_AND_ALGO = 3;
    private static final int SPATIAL_AUDIO_TYPE_SUPPORT_GYRO_ONLY = 2;
    private static final String TAG = "AS.BtHelperImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BtHelperImpl> {

        /* compiled from: BtHelperImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BtHelperImpl INSTANCE = new BtHelperImpl();
        }

        public BtHelperImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BtHelperImpl provideNewInstance() {
            return new BtHelperImpl();
        }
    }

    public boolean checkEncoderFormat(BluetoothCodecConfig btCodecConfig) {
        if (btCodecConfig.getEncoderFormat() == AUDIO_FORMAT_FORCE_AOSP) {
            return true;
        }
        return false;
    }

    public boolean handleSpatialAudioDeviceConnect(BluetoothDevice spatialDevice, BluetoothDevice currActiveDevice, int spatialAudioType) {
        Log.i(TAG, "spatialAudioType: " + spatialAudioType);
        if (Objects.equals(spatialDevice, currActiveDevice)) {
            if (spatialAudioType == 2 || spatialAudioType == 3) {
                Log.d(TAG, "spatial audio device connect");
                AudioSystem.setParameters("spatial_audio_headphone_connect=true");
                return true;
            }
            return false;
        }
        return false;
    }

    public void handleSpatialAudioDeviceDisConnect(BluetoothDevice spatialDevice, BluetoothDevice previousActiveDevice) {
        if (spatialDevice != null && Objects.equals(spatialDevice, previousActiveDevice)) {
            Log.d(TAG, "change to not spatial audio device");
            AudioSystem.setParameters("spatial_audio_headphone_connect=false");
        }
    }
}
