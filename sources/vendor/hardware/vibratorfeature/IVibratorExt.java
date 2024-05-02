package vendor.hardware.vibratorfeature;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IVibratorExt extends IInterface {
    public static final String DESCRIPTOR = "vendor$hardware$vibratorfeature$IVibratorExt".replace('$', '.');
    public static final String HASH = "e5c4e95700e5402f68bcbec67acb46d1cb2a365a";
    public static final int VERSION = 1;

    void configStrengthForEffect(int i, float f) throws RemoteException;

    String getInterfaceHash() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void pause() throws RemoteException;

    void play(int[] iArr, int i, long j, int i2) throws RemoteException;

    void seekTo(long j) throws RemoteException;

    void setAmplitudeExt(float f, int i) throws RemoteException;

    void stop() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IVibratorExt {
        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void play(int[] effect, int loop, long interval, int amplitude) throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void pause() throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void stop() throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void seekTo(long time) throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void setAmplitudeExt(float amplitude, int flag) throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public void configStrengthForEffect(int id, float strength) throws RemoteException {
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public int getInterfaceVersion() {
            return 0;
        }

        @Override // vendor.hardware.vibratorfeature.IVibratorExt
        public String getInterfaceHash() {
            return "";
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IVibratorExt {
        static final int TRANSACTION_configStrengthForEffect = 6;
        static final int TRANSACTION_getInterfaceHash = 16777214;
        static final int TRANSACTION_getInterfaceVersion = 16777215;
        static final int TRANSACTION_pause = 2;
        static final int TRANSACTION_play = 1;
        static final int TRANSACTION_seekTo = 4;
        static final int TRANSACTION_setAmplitudeExt = 5;
        static final int TRANSACTION_stop = 3;

        public Stub() {
            markVintfStability();
            attachInterface(this, DESCRIPTOR);
        }

        public static IVibratorExt asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IVibratorExt)) {
                return (IVibratorExt) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= 1 && code <= TRANSACTION_getInterfaceVersion) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case TRANSACTION_getInterfaceHash /* 16777214 */:
                    reply.writeNoException();
                    reply.writeString(getInterfaceHash());
                    return true;
                case TRANSACTION_getInterfaceVersion /* 16777215 */:
                    reply.writeNoException();
                    reply.writeInt(getInterfaceVersion());
                    return true;
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            int[] _arg0 = data.createIntArray();
                            int _arg1 = data.readInt();
                            long _arg2 = data.readLong();
                            int _arg3 = data.readInt();
                            data.enforceNoDataAvail();
                            play(_arg0, _arg1, _arg2, _arg3);
                            reply.writeNoException();
                            break;
                        case 2:
                            pause();
                            reply.writeNoException();
                            break;
                        case 3:
                            stop();
                            reply.writeNoException();
                            break;
                        case 4:
                            long _arg02 = data.readLong();
                            data.enforceNoDataAvail();
                            seekTo(_arg02);
                            reply.writeNoException();
                            break;
                        case 5:
                            float _arg03 = data.readFloat();
                            int _arg12 = data.readInt();
                            data.enforceNoDataAvail();
                            setAmplitudeExt(_arg03, _arg12);
                            reply.writeNoException();
                            break;
                        case 6:
                            int _arg04 = data.readInt();
                            float _arg13 = data.readFloat();
                            data.enforceNoDataAvail();
                            configStrengthForEffect(_arg04, _arg13);
                            reply.writeNoException();
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IVibratorExt {
            private IBinder mRemote;
            private int mCachedVersion = -1;
            private String mCachedHash = "-1";

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void play(int[] effect, int loop, long interval, int amplitude) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeIntArray(effect);
                    _data.writeInt(loop);
                    _data.writeLong(interval);
                    _data.writeInt(amplitude);
                    boolean _status = this.mRemote.transact(1, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method play is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void pause() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(2, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method pause is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void stop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = this.mRemote.transact(3, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method stop is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void seekTo(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(time);
                    boolean _status = this.mRemote.transact(4, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method seekTo is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void setAmplitudeExt(float amplitude, int flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeFloat(amplitude);
                    _data.writeInt(flag);
                    boolean _status = this.mRemote.transact(5, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method setAmplitudeExt is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public void configStrengthForEffect(int id, float strength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(id);
                    _data.writeFloat(strength);
                    boolean _status = this.mRemote.transact(6, _data, _reply, 0);
                    if (!_status) {
                        throw new RemoteException("Method configStrengthForEffect is unimplemented.");
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(DESCRIPTOR);
                        this.mRemote.transact(Stub.TRANSACTION_getInterfaceVersion, data, reply, 0);
                        reply.readException();
                        this.mCachedVersion = reply.readInt();
                    } finally {
                        reply.recycle();
                        data.recycle();
                    }
                }
                return this.mCachedVersion;
            }

            @Override // vendor.hardware.vibratorfeature.IVibratorExt
            public synchronized String getInterfaceHash() throws RemoteException {
                if ("-1".equals(this.mCachedHash)) {
                    Parcel data = Parcel.obtain();
                    Parcel reply = Parcel.obtain();
                    data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getInterfaceHash, data, reply, 0);
                    reply.readException();
                    this.mCachedHash = reply.readString();
                    reply.recycle();
                    data.recycle();
                }
                return this.mCachedHash;
            }
        }
    }
}
