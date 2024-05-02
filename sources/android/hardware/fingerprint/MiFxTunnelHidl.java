package android.hardware.fingerprint;

import android.content.Context;
import android.os.Handler;
import android.os.IHwBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.biometrics.BiometricServiceBaseStub;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.fingerprint.MiuiFingerprintServiceStubImpl;
import java.util.ArrayList;
import vendor.xiaomi.hardware.fx.tunnel.V1_0.IMiFxTunnel;
import vendor.xiaomi.hardware.fx.tunnel.V1_0.IMiFxTunnelCallback;
/* loaded from: classes.dex */
public class MiFxTunnelHidl {
    private static final String TAG = "FingerprintServiceHidl";
    private static volatile MiFxTunnelHidl sInstance;
    private Handler mHandler;
    private IMiFxTunnel miFxTunnel = null;
    private IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() { // from class: android.hardware.fingerprint.MiFxTunnelHidl.1
        public void serviceDied(long cookie) {
            if (MiFxTunnelHidl.this.miFxTunnel == null) {
                return;
            }
            try {
                MiFxTunnelHidl.this.miFxTunnel.unlinkToDeath(MiFxTunnelHidl.this.mDeathRecipient);
                MiFxTunnelHidl.this.miFxTunnel = null;
                MiFxTunnelHidl miFxTunnelHidl = MiFxTunnelHidl.this;
                miFxTunnelHidl.miFxTunnel = miFxTunnelHidl.getMiFxTunnel();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };
    private IMiFxTunnelCallback mMiFxTunnelCallback = new IMiFxTunnelCallback.Stub() { // from class: android.hardware.fingerprint.MiFxTunnelHidl.3
        public void onMessage(long devId, int msgId, int cmdId, ArrayList<Byte> msg_data) throws RemoteException {
            if (devId == -1 && msgId == -1 && cmdId == -1) {
                Slog.d(MiFxTunnelHidl.TAG, "onMessage: callback has been replaced!");
            } else {
                MiuiFingerprintServiceStubImpl.heartRateDataCallback(msgId, cmdId, MiFxTunnelHidl.arrayListToByteArray(msg_data));
            }
        }
    };

    private MiFxTunnelHidl() {
    }

    public static MiFxTunnelHidl getInstance() {
        Slog.d(TAG, "getInstance");
        if (sInstance == null) {
            Slog.d(TAG, "getInstance null");
            synchronized (MiFxTunnelHidl.class) {
                Slog.d(TAG, "getInstance class");
                if (sInstance == null) {
                    Slog.d(TAG, "getInstance null 2");
                    sInstance = new MiFxTunnelHidl();
                }
            }
        }
        Slog.d(TAG, "getInstance out");
        return sInstance;
    }

    public IMiFxTunnel getMiFxTunnel() throws RemoteException {
        if (this.miFxTunnel == null) {
            IMiFxTunnel service = IMiFxTunnel.getService();
            this.miFxTunnel = service;
            service.linkToDeath(this.mDeathRecipient, 0L);
        }
        return this.miFxTunnel;
    }

    public synchronized HeartRateCmdResult sendCommand(final int cmdId, byte[] params) throws RemoteException {
        final HeartRateCmdResult result;
        IMiFxTunnel miFxTunnel = getMiFxTunnel();
        result = new HeartRateCmdResult();
        if (miFxTunnel != null) {
            miFxTunnel.invokeCommand(cmdId, byteArrayToArrayList(params), new IMiFxTunnel.invokeCommandCallback() { // from class: android.hardware.fingerprint.MiFxTunnelHidl.2
                public void onValues(int resultCode, ArrayList<Byte> out_buf) {
                    if (resultCode == 0) {
                        BiometricServiceBaseStub.getInstance().fodCallBack((Context) null, cmdId, 0, "com.mi.health", (BaseClientMonitor) null);
                        Message msg = MiFxTunnelHidl.this.mHandler.obtainMessage(cmdId);
                        msg.arg1 = cmdId;
                        msg.sendToTarget();
                        result.mResultCode = resultCode;
                        result.mResultData = MiFxTunnelHidl.arrayListToByteArray(out_buf);
                        return;
                    }
                    result.mResultCode = resultCode;
                    result.mResultData = null;
                }
            });
        }
        return result;
    }

    public void registerCallback(Handler handler) {
        try {
            this.mHandler = handler;
            IMiFxTunnel miFxTunnel = getMiFxTunnel();
            miFxTunnel.setNotify(this.mMiFxTunnelCallback);
        } catch (Exception e) {
            Slog.d(TAG, "registerCallback err");
        }
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] array) {
        ArrayList<Byte> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (byte b : array) {
            Byte b2 = Byte.valueOf(b);
            list.add(b2);
        }
        return list;
    }

    public static byte[] arrayListToByteArray(ArrayList<Byte> list) {
        Byte[] byteArray = (Byte[]) list.toArray(new Byte[list.size()]);
        int i = 0;
        byte[] result = new byte[list.size()];
        int length = byteArray.length;
        int i2 = 0;
        while (i2 < length) {
            Byte b = byteArray[i2];
            result[i] = b.byteValue();
            i2++;
            i++;
        }
        return result;
    }
}
