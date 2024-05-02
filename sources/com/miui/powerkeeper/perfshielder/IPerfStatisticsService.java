package com.miui.powerkeeper.perfshielder;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IPerfStatisticsService extends IInterface {
    void reportActivityLaunchTime(String str, long j) throws RemoteException;

    void reportFocusChanged(String str, String str2) throws RemoteException;

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IPerfStatisticsService {
        private static final String DESCRIPTOR = "com.miui.powerkeeper.perfshielder.IPerfStatisticsService";
        static final int TRANSACTION_reportActivityLaunchTime = 2;
        static final int TRANSACTION_reportFocusChanged = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPerfStatisticsService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPerfStatisticsService)) {
                return (IPerfStatisticsService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String pkgName = data.readString();
                    String windowName = data.readString();
                    reportFocusChanged(pkgName, windowName);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String pkgName2 = data.readString();
                    long duration = data.readLong();
                    reportActivityLaunchTime(pkgName2, duration);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IPerfStatisticsService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // com.miui.powerkeeper.perfshielder.IPerfStatisticsService
            public void reportFocusChanged(String pkgName, String windowName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeString(windowName);
                    this.mRemote.transact(1, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.powerkeeper.perfshielder.IPerfStatisticsService
            public void reportActivityLaunchTime(String pkgName, long duration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    _data.writeLong(duration);
                    this.mRemote.transact(2, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
