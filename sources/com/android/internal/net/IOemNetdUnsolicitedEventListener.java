package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
/* loaded from: classes.dex */
public interface IOemNetdUnsolicitedEventListener extends IInterface {
    public static final String DESCRIPTOR = "com$android$internal$net$IOemNetdUnsolicitedEventListener".replace('$', '.');

    void onFirewallBlocked(int i, String str) throws RemoteException;

    void onRegistered() throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetdUnsolicitedEventListener {
        @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
        public void onRegistered() throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
        public void onFirewallBlocked(int code, String packageName) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetdUnsolicitedEventListener {
        static final int TRANSACTION_onFirewallBlocked = 2;
        static final int TRANSACTION_onRegistered = 1;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetdUnsolicitedEventListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetdUnsolicitedEventListener)) {
                return (IOemNetdUnsolicitedEventListener) iin;
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
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(descriptor);
            }
            switch (code) {
                case 1598968902:
                    reply.writeString(descriptor);
                    return true;
                default:
                    switch (code) {
                        case 1:
                            onRegistered();
                            break;
                        case 2:
                            int _arg0 = data.readInt();
                            String _arg1 = data.readString();
                            onFirewallBlocked(_arg0, _arg1);
                            break;
                        default:
                            return super.onTransact(code, data, reply, flags);
                    }
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public static class Proxy implements IOemNetdUnsolicitedEventListener {
            private IBinder mRemote;

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

            @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
            public void onRegistered() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetdUnsolicitedEventListener
            public void onFirewallBlocked(int code, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
