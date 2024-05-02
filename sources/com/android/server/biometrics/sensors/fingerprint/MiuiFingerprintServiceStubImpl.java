package com.android.server.biometrics.sensors.fingerprint;

import android.hardware.fingerprint.HeartRateCmdResult;
import android.hardware.fingerprint.MiFxTunnelHidl;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.server.policy.DisplayTurnoverManager;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MiuiFingerprintServiceStubImpl implements MiuiFingerprintServiceStub {
    private static final boolean DEBUG = true;
    private static final String TAG = "FingerprintService";
    private static IBinder mHeartRateBinder = null;
    private Handler mHandler;
    private FingerprintService mService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFingerprintServiceStubImpl> {

        /* compiled from: MiuiFingerprintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFingerprintServiceStubImpl INSTANCE = new MiuiFingerprintServiceStubImpl();
        }

        public MiuiFingerprintServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiFingerprintServiceStubImpl provideNewInstance() {
            return new MiuiFingerprintServiceStubImpl();
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        try {
            switch (code) {
                case 16777213:
                    mHeartRateBinder = null;
                    reply.writeNoException();
                    return true;
                case 16777214:
                    data.enforceInterface("com.android.app.HeartRate");
                    mHeartRateBinder = data.readStrongBinder();
                    registerCallback();
                    reply.writeNoException();
                    return true;
                case 16777215:
                    data.enforceInterface("com.android.app.HeartRate");
                    int cmd = data.readInt();
                    int size = data.readInt();
                    byte[] val = null;
                    if (size >= 0) {
                        val = new byte[size];
                        data.readByteArray(val);
                    }
                    HeartRateCmdResult result = doSendCommand(cmd, val);
                    reply.writeParcelable(result, 1);
                    return true;
                default:
                    return true;
            }
        } catch (RemoteException e) {
            Slog.d(TAG, "onTra : ", e);
            return false;
        }
    }

    public HeartRateCmdResult doSendCommand(int cmdId, byte[] params) throws RemoteException {
        return MiFxTunnelHidl.getInstance().sendCommand(cmdId, params);
    }

    private void registerCallback() throws RemoteException {
        Slog.d(TAG, "reg callback");
        if (MiFxTunnelHidl.getInstance() == null) {
            Slog.d(TAG, "get null");
            return;
        }
        Slog.d(TAG, "get reg");
        MiFxTunnelHidl.getInstance().registerCallback(this.mHandler);
    }

    public static boolean heartRateDataCallback(int msgId, int cmdId, byte[] msg_data) {
        Log.d(TAG, "heartRateDataCallback: msgId: " + msgId + " cmdId: " + cmdId + " msg_data: " + msg_data + " mHeartRateBinder: " + mHeartRateBinder);
        if (mHeartRateBinder != null) {
            Parcel request = Parcel.obtain();
            try {
                request.writeInterfaceToken("com.android.app.HeartRate");
                request.writeInt(msgId);
                request.writeInt(cmdId);
                request.writeByteArray(msg_data);
                mHeartRateBinder.transact(DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY, request, null, 1);
                return true;
            } catch (Exception e) {
                mHeartRateBinder = null;
                e.printStackTrace();
                return false;
            } finally {
                request.recycle();
            }
        }
        return false;
    }
}
