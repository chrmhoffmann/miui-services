package com.android.internal.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.net.IOemNetdUnsolicitedEventListener;
/* loaded from: classes.dex */
public interface IOemNetd extends IInterface {
    public static final String DESCRIPTOR = "com$android$internal$net$IOemNetd".replace('$', '.');

    boolean addMiuiFirewallSharedUid(int i) throws RemoteException;

    int enableAutoForward(String str, int i, boolean z) throws RemoteException;

    boolean enableIptablesRestore(boolean z) throws RemoteException;

    boolean enableLimitter(boolean z) throws RemoteException;

    boolean enableMobileTrafficLimit(boolean z, String str) throws RemoteException;

    boolean enableQos(boolean z) throws RemoteException;

    boolean enableRps(String str, boolean z) throws RemoteException;

    boolean enableWmmer(boolean z) throws RemoteException;

    long getMiuiSlmVoipUdpAddress(int i) throws RemoteException;

    int getMiuiSlmVoipUdpPort(int i) throws RemoteException;

    boolean isAlive() throws RemoteException;

    boolean listenUidDataActivity(int i, int i2, int i3, int i4, boolean z) throws RemoteException;

    void notifyFirewallBlocked(int i, String str) throws RemoteException;

    void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener iOemNetdUnsolicitedEventListener) throws RemoteException;

    boolean setCurrentNetworkState(int i) throws RemoteException;

    boolean setLimit(boolean z, long j) throws RemoteException;

    boolean setMiuiFirewallRule(String str, int i, int i2, int i3) throws RemoteException;

    boolean setMiuiSlmBpfUid(int i) throws RemoteException;

    boolean setMobileTrafficLimit(boolean z, long j) throws RemoteException;

    void setPidForPackage(String str, int i, int i2) throws RemoteException;

    boolean setQos(int i, int i2, int i3, boolean z) throws RemoteException;

    boolean updateIface(String str) throws RemoteException;

    boolean updateWmm(int i, int i2) throws RemoteException;

    boolean whiteListUid(int i, boolean z) throws RemoteException;

    boolean whiteListUidForMobileTraffic(int i, boolean z) throws RemoteException;

    /* loaded from: classes.dex */
    public static class Default implements IOemNetd {
        @Override // com.android.internal.net.IOemNetd
        public boolean isAlive() throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableWmmer(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableLimitter(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean updateWmm(int uid, int wmm) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean whiteListUid(int uid, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setLimit(boolean enabled, long rate) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableIptablesRestore(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean listenUidDataActivity(int protocol, int uid, int label, int timeout, boolean listen) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean updateIface(String iface) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean addMiuiFirewallSharedUid(int uid) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setCurrentNetworkState(int state) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableRps(String iface, boolean enable) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void notifyFirewallBlocked(int code, String packageName) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableQos(boolean enabled) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setQos(int protocol, int uid, int tos, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public void setPidForPackage(String packageName, int pid, int uid) throws RemoteException {
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean enableMobileTrafficLimit(boolean enabled, String iface) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMobileTrafficLimit(boolean enabled, long rate) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean whiteListUidForMobileTraffic(int uid, boolean add) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public boolean setMiuiSlmBpfUid(int uid) throws RemoteException {
            return false;
        }

        @Override // com.android.internal.net.IOemNetd
        public long getMiuiSlmVoipUdpAddress(int uid) throws RemoteException {
            return 0L;
        }

        @Override // com.android.internal.net.IOemNetd
        public int getMiuiSlmVoipUdpPort(int uid) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public int enableAutoForward(String addr, int fwmark, boolean enabled) throws RemoteException {
            return 0;
        }

        @Override // com.android.internal.net.IOemNetd
        public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IOemNetd {
        static final int TRANSACTION_addMiuiFirewallSharedUid = 10;
        static final int TRANSACTION_enableAutoForward = 24;
        static final int TRANSACTION_enableIptablesRestore = 7;
        static final int TRANSACTION_enableLimitter = 3;
        static final int TRANSACTION_enableMobileTrafficLimit = 18;
        static final int TRANSACTION_enableQos = 15;
        static final int TRANSACTION_enableRps = 13;
        static final int TRANSACTION_enableWmmer = 2;
        static final int TRANSACTION_getMiuiSlmVoipUdpAddress = 22;
        static final int TRANSACTION_getMiuiSlmVoipUdpPort = 23;
        static final int TRANSACTION_isAlive = 1;
        static final int TRANSACTION_listenUidDataActivity = 8;
        static final int TRANSACTION_notifyFirewallBlocked = 14;
        static final int TRANSACTION_registerOemUnsolicitedEventListener = 25;
        static final int TRANSACTION_setCurrentNetworkState = 12;
        static final int TRANSACTION_setLimit = 6;
        static final int TRANSACTION_setMiuiFirewallRule = 11;
        static final int TRANSACTION_setMiuiSlmBpfUid = 21;
        static final int TRANSACTION_setMobileTrafficLimit = 19;
        static final int TRANSACTION_setPidForPackage = 17;
        static final int TRANSACTION_setQos = 16;
        static final int TRANSACTION_updateIface = 9;
        static final int TRANSACTION_updateWmm = 4;
        static final int TRANSACTION_whiteListUid = 5;
        static final int TRANSACTION_whiteListUidForMobileTraffic = 20;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IOemNetd asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IOemNetd)) {
                return (IOemNetd) iin;
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
                            boolean _result = isAlive();
                            reply.writeNoException();
                            reply.writeBoolean(_result);
                            break;
                        case 2:
                            boolean _arg0 = data.readBoolean();
                            boolean _result2 = enableWmmer(_arg0);
                            reply.writeNoException();
                            reply.writeBoolean(_result2);
                            break;
                        case 3:
                            boolean _arg02 = data.readBoolean();
                            boolean _result3 = enableLimitter(_arg02);
                            reply.writeNoException();
                            reply.writeBoolean(_result3);
                            break;
                        case 4:
                            int _arg03 = data.readInt();
                            int _arg1 = data.readInt();
                            boolean _result4 = updateWmm(_arg03, _arg1);
                            reply.writeNoException();
                            reply.writeBoolean(_result4);
                            break;
                        case 5:
                            int _arg04 = data.readInt();
                            boolean _arg12 = data.readBoolean();
                            boolean _result5 = whiteListUid(_arg04, _arg12);
                            reply.writeNoException();
                            reply.writeBoolean(_result5);
                            break;
                        case 6:
                            boolean _arg05 = data.readBoolean();
                            long _arg13 = data.readLong();
                            boolean _result6 = setLimit(_arg05, _arg13);
                            reply.writeNoException();
                            reply.writeBoolean(_result6);
                            break;
                        case 7:
                            boolean _arg06 = data.readBoolean();
                            boolean _result7 = enableIptablesRestore(_arg06);
                            reply.writeNoException();
                            reply.writeBoolean(_result7);
                            break;
                        case 8:
                            int _arg07 = data.readInt();
                            int _arg14 = data.readInt();
                            int _arg2 = data.readInt();
                            int _arg3 = data.readInt();
                            boolean _arg4 = data.readBoolean();
                            boolean _result8 = listenUidDataActivity(_arg07, _arg14, _arg2, _arg3, _arg4);
                            reply.writeNoException();
                            reply.writeBoolean(_result8);
                            break;
                        case 9:
                            String _arg08 = data.readString();
                            boolean _result9 = updateIface(_arg08);
                            reply.writeNoException();
                            reply.writeBoolean(_result9);
                            break;
                        case 10:
                            int _arg09 = data.readInt();
                            boolean _result10 = addMiuiFirewallSharedUid(_arg09);
                            reply.writeNoException();
                            reply.writeBoolean(_result10);
                            break;
                        case 11:
                            String _arg010 = data.readString();
                            int _arg15 = data.readInt();
                            int _arg22 = data.readInt();
                            int _arg32 = data.readInt();
                            boolean _result11 = setMiuiFirewallRule(_arg010, _arg15, _arg22, _arg32);
                            reply.writeNoException();
                            reply.writeBoolean(_result11);
                            break;
                        case 12:
                            int _arg011 = data.readInt();
                            boolean _result12 = setCurrentNetworkState(_arg011);
                            reply.writeNoException();
                            reply.writeBoolean(_result12);
                            break;
                        case 13:
                            String _arg012 = data.readString();
                            boolean _arg16 = data.readBoolean();
                            boolean _result13 = enableRps(_arg012, _arg16);
                            reply.writeNoException();
                            reply.writeBoolean(_result13);
                            break;
                        case 14:
                            int _arg013 = data.readInt();
                            String _arg17 = data.readString();
                            notifyFirewallBlocked(_arg013, _arg17);
                            reply.writeNoException();
                            break;
                        case 15:
                            boolean _arg014 = data.readBoolean();
                            boolean _result14 = enableQos(_arg014);
                            reply.writeNoException();
                            reply.writeBoolean(_result14);
                            break;
                        case 16:
                            int _arg015 = data.readInt();
                            int _arg18 = data.readInt();
                            int _arg23 = data.readInt();
                            boolean _arg33 = data.readBoolean();
                            boolean _result15 = setQos(_arg015, _arg18, _arg23, _arg33);
                            reply.writeNoException();
                            reply.writeBoolean(_result15);
                            break;
                        case 17:
                            String _arg016 = data.readString();
                            int _arg19 = data.readInt();
                            int _arg24 = data.readInt();
                            setPidForPackage(_arg016, _arg19, _arg24);
                            break;
                        case 18:
                            boolean _arg017 = data.readBoolean();
                            String _arg110 = data.readString();
                            boolean _result16 = enableMobileTrafficLimit(_arg017, _arg110);
                            reply.writeNoException();
                            reply.writeBoolean(_result16);
                            break;
                        case 19:
                            boolean _arg018 = data.readBoolean();
                            long _arg111 = data.readLong();
                            boolean _result17 = setMobileTrafficLimit(_arg018, _arg111);
                            reply.writeNoException();
                            reply.writeBoolean(_result17);
                            break;
                        case 20:
                            int _arg019 = data.readInt();
                            boolean _arg112 = data.readBoolean();
                            boolean _result18 = whiteListUidForMobileTraffic(_arg019, _arg112);
                            reply.writeNoException();
                            reply.writeBoolean(_result18);
                            break;
                        case 21:
                            int _arg020 = data.readInt();
                            boolean _result19 = setMiuiSlmBpfUid(_arg020);
                            reply.writeNoException();
                            reply.writeBoolean(_result19);
                            break;
                        case 22:
                            int _arg021 = data.readInt();
                            long _result20 = getMiuiSlmVoipUdpAddress(_arg021);
                            reply.writeNoException();
                            reply.writeLong(_result20);
                            break;
                        case 23:
                            int _arg022 = data.readInt();
                            int _result21 = getMiuiSlmVoipUdpPort(_arg022);
                            reply.writeNoException();
                            reply.writeInt(_result21);
                            break;
                        case 24:
                            String _arg023 = data.readString();
                            int _arg113 = data.readInt();
                            boolean _arg25 = data.readBoolean();
                            int _result22 = enableAutoForward(_arg023, _arg113, _arg25);
                            reply.writeNoException();
                            reply.writeInt(_result22);
                            break;
                        case 25:
                            IOemNetdUnsolicitedEventListener _arg024 = IOemNetdUnsolicitedEventListener.Stub.asInterface(data.readStrongBinder());
                            registerOemUnsolicitedEventListener(_arg024);
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
        public static class Proxy implements IOemNetd {
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

            @Override // com.android.internal.net.IOemNetd
            public boolean isAlive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableWmmer(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableLimitter(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean updateWmm(int uid, int wmm) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(wmm);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean whiteListUid(int uid, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeBoolean(add);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setLimit(boolean enabled, long rate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeLong(rate);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableIptablesRestore(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean listenUidDataActivity(int protocol, int uid, int label, int timeout, boolean listen) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(protocol);
                    _data.writeInt(uid);
                    _data.writeInt(label);
                    _data.writeInt(timeout);
                    _data.writeBoolean(listen);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean updateIface(String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(iface);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean addMiuiFirewallSharedUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(uid);
                    _data.writeInt(rule);
                    _data.writeInt(type);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setCurrentNetworkState(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableRps(String iface, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(iface);
                    _data.writeBoolean(enable);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void notifyFirewallBlocked(int code, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(code);
                    _data.writeString(packageName);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableQos(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setQos(int protocol, int uid, int tos, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(protocol);
                    _data.writeInt(uid);
                    _data.writeInt(tos);
                    _data.writeBoolean(add);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void setPidForPackage(String packageName, int pid, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean enableMobileTrafficLimit(boolean enabled, String iface) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeString(iface);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMobileTrafficLimit(boolean enabled, long rate) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeBoolean(enabled);
                    _data.writeLong(rate);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean whiteListUidForMobileTraffic(int uid, boolean add) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeBoolean(add);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public boolean setMiuiSlmBpfUid(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readBoolean();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public long getMiuiSlmVoipUdpAddress(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int getMiuiSlmVoipUdpPort(int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public int enableAutoForward(String addr, int fwmark, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(addr);
                    _data.writeInt(fwmark);
                    _data.writeBoolean(enabled);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.android.internal.net.IOemNetd
            public void registerOemUnsolicitedEventListener(IOemNetdUnsolicitedEventListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongInterface(listener);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
