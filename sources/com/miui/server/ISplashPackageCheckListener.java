package com.miui.server;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;
/* loaded from: classes.dex */
public interface ISplashPackageCheckListener extends IInterface {
    void updateSplashPackageCheckInfo(SplashPackageCheckInfo splashPackageCheckInfo) throws RemoteException;

    void updateSplashPackageCheckInfoList(List<SplashPackageCheckInfo> list) throws RemoteException;

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements ISplashPackageCheckListener {
        private static final String DESCRIPTOR = "com.miui.server.ISplashPackageCheckListener";
        static final int TRANSACTION_updateSplashPackageCheckInfo = 2;
        static final int TRANSACTION_updateSplashPackageCheckInfoList = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISplashPackageCheckListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ISplashPackageCheckListener)) {
                return (ISplashPackageCheckListener) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            SplashPackageCheckInfo _arg0;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    List<SplashPackageCheckInfo> _arg02 = data.createTypedArrayList(SplashPackageCheckInfo.CREATOR);
                    updateSplashPackageCheckInfoList(_arg02);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = SplashPackageCheckInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    updateSplashPackageCheckInfo(_arg0);
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
        public static class Proxy implements ISplashPackageCheckListener {
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

            @Override // com.miui.server.ISplashPackageCheckListener
            public void updateSplashPackageCheckInfoList(List<SplashPackageCheckInfo> splashPackageCheckInfos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(splashPackageCheckInfos);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.miui.server.ISplashPackageCheckListener
            public void updateSplashPackageCheckInfo(SplashPackageCheckInfo splashPackageCheckInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (splashPackageCheckInfo != null) {
                        _data.writeInt(1);
                        splashPackageCheckInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
