package com.miui.server;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.miui.server.ISplashPackageCheckListener;
/* loaded from: classes.dex */
public interface ISplashScreenService extends IInterface {
    void activityIdle(ActivityInfo activityInfo) throws RemoteException;

    void destroyActivity(ActivityInfo activityInfo) throws RemoteException;

    Intent requestSplashScreen(Intent intent, ActivityInfo activityInfo) throws RemoteException;

    void setSplashPackageListener(ISplashPackageCheckListener iSplashPackageCheckListener) throws RemoteException;

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISplashScreenService {
        private static final String DESCRIPTOR = "com.miui.server.ISplashScreenService";
        static final int TRANSACTION_activityIdle = 2;
        static final int TRANSACTION_destroyActivity = 3;
        static final int TRANSACTION_requestSplashScreen = 1;
        static final int TRANSACTION_setSplashPackageListener = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISplashScreenService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISplashScreenService)) {
                return (ISplashScreenService) iin;
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
                    Intent intent = (Intent) Intent.CREATOR.createFromParcel(data);
                    ActivityInfo aInfo = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(data);
                    Intent _result = requestSplashScreen(intent, aInfo);
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    ActivityInfo aInfo2 = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(data);
                    activityIdle(aInfo2);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    ActivityInfo aInfo3 = (ActivityInfo) ActivityInfo.CREATOR.createFromParcel(data);
                    destroyActivity(aInfo3);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    ISplashPackageCheckListener _arg0 = ISplashPackageCheckListener.Stub.asInterface(data.readStrongBinder());
                    setSplashPackageListener(_arg0);
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
        public static class Proxy implements ISplashScreenService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // com.miui.server.ISplashScreenService
            public Intent requestSplashScreen(Intent intent, ActivityInfo aInfo) throws RemoteException {
                Intent _result;
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                intent.writeToParcel(_data, 0);
                aInfo.writeToParcel(_data, 0);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Intent) Intent.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.server.ISplashScreenService
            public void activityIdle(ActivityInfo aInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                aInfo.writeToParcel(_data, 0);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.server.ISplashScreenService
            public void destroyActivity(ActivityInfo aInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                aInfo.writeToParcel(_data, 0);
                try {
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.miui.server.ISplashScreenService
            public void setSplashPackageListener(ISplashPackageCheckListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
