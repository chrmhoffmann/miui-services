package com.android.server.biometrics.sensors.face;

import android.os.HidlSupport;
import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.IHwInterface;
import android.os.NativeHandle;
import android.os.RemoteException;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import miui.android.services.internal.hidl.base.V1_0.DebugInfo;
import miui.android.services.internal.hidl.base.V1_0.IBase;
/* loaded from: classes.dex */
public interface IMiFaceSession extends IBase {
    public static final String kInterfaceName = "vendor.xiaomi.hardware.miface@1.0::IMiFaceSession";

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    IHwBinder asBinder();

    int authenticate(long j) throws RemoteException;

    int cancel() throws RemoteException;

    int close() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void debug(NativeHandle nativeHandle, ArrayList<String> arrayList) throws RemoteException;

    int enroll(ArrayList<Byte> arrayList, ArrayList<Integer> arrayList2, NativeHandle nativeHandle) throws RemoteException;

    int enumerateEnrollments() throws RemoteException;

    long generateChallenge() throws RemoteException;

    long getAuthenticatorId() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    DebugInfo getDebugInfo() throws RemoteException;

    int getFeatures() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    ArrayList<byte[]> getHashChain() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    ArrayList<String> interfaceChain() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    String interfaceDescriptor() throws RemoteException;

    int invalidateAuthenticatorId() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    boolean linkToDeath(IHwBinder.DeathRecipient deathRecipient, long j) throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void notifySyspropsChanged() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void ping() throws RemoteException;

    int removeEnrollments(ArrayList<Integer> arrayList) throws RemoteException;

    int resetLockout(ArrayList<Byte> arrayList) throws RemoteException;

    int revokeChallenge(long j) throws RemoteException;

    int setFeature(ArrayList<Byte> arrayList, int i, boolean z) throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    void setHALInstrumentation() throws RemoteException;

    @Override // miui.android.services.internal.hidl.base.V1_0.IBase
    boolean unlinkToDeath(IHwBinder.DeathRecipient deathRecipient) throws RemoteException;

    static IMiFaceSession asInterface(IHwBinder binder) {
        if (binder == null) {
            return null;
        }
        IHwInterface iface = binder.queryLocalInterface(kInterfaceName);
        if (iface != null && (iface instanceof IMiFaceSession)) {
            return (IMiFaceSession) iface;
        }
        IMiFaceSession proxy = new Proxy(binder);
        try {
            Iterator<String> it = proxy.interfaceChain().iterator();
            while (it.hasNext()) {
                String descriptor = it.next();
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    static IMiFaceSession castFrom(IHwInterface iface) {
        if (iface == null) {
            return null;
        }
        return asInterface(iface.asBinder());
    }

    static IMiFaceSession getService(String serviceName, boolean retry) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName, retry));
    }

    static IMiFaceSession getService(boolean retry) throws RemoteException {
        return getService("default", retry);
    }

    @Deprecated
    static IMiFaceSession getService(String serviceName) throws RemoteException {
        return asInterface(HwBinder.getService(kInterfaceName, serviceName));
    }

    @Deprecated
    static IMiFaceSession getService() throws RemoteException {
        return getService("default");
    }

    /* loaded from: classes.dex */
    public static final class Proxy implements IMiFaceSession {
        private IHwBinder mRemote;

        public Proxy(IHwBinder remote) {
            this.mRemote = (IHwBinder) Objects.requireNonNull(remote);
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this.mRemote;
        }

        public String toString() {
            try {
                return interfaceDescriptor() + "@Proxy";
            } catch (RemoteException e) {
                return "[class or subclass of vendor.xiaomi.hardware.miface@1.0::IMiFaceSession]@Proxy";
            }
        }

        public final boolean equals(Object other) {
            return HidlSupport.interfacesEqual(this, other);
        }

        public final int hashCode() {
            return asBinder().hashCode();
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public long generateChallenge() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(1, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_status = _hidl_reply.readInt64();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int revokeChallenge(long challenge) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt64(challenge);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(2, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int enroll(ArrayList<Byte> hat, ArrayList<Integer> features, NativeHandle previewSurface) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt8Vector(hat);
            _hidl_request.writeInt32Vector(features);
            _hidl_request.writeNativeHandle(previewSurface);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(3, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int authenticate(long operationId) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt64(operationId);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(4, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int enumerateEnrollments() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(5, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int removeEnrollments(ArrayList<Integer> enrollmentIds) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt32Vector(enrollmentIds);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(6, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int getFeatures() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(7, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int setFeature(ArrayList<Byte> hat, int feature, boolean enabled) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt8Vector(hat);
            _hidl_request.writeInt32(feature);
            _hidl_request.writeBool(enabled);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(8, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public long getAuthenticatorId() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(9, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                long _hidl_out_result = _hidl_reply.readInt64();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int invalidateAuthenticatorId() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(10, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int resetLockout(ArrayList<Byte> hat) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            _hidl_request.writeInt8Vector(hat);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(11, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int close() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(12, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession
        public int cancel() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IMiFaceSession.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(13, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                int _hidl_out_status = _hidl_reply.readInt32();
                return _hidl_out_status;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public ArrayList<String> interfaceChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256067662, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256131655, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public String interfaceDescriptor() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256136003, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public ArrayList<byte[]> getHashChain() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256398152, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                ArrayList<byte[]> _hidl_out_hashchain = new ArrayList<>();
                HwBlob _hidl_blob = _hidl_reply.readBuffer(16L);
                int _hidl_vec_size = _hidl_blob.getInt32(8L);
                HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(_hidl_vec_size * 32, _hidl_blob.handle(), 0L, true);
                _hidl_out_hashchain.clear();
                for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                    byte[] _hidl_vec_element = new byte[32];
                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                    childBlob.copyToInt8Array(_hidl_array_offset_1, _hidl_vec_element, 32);
                    _hidl_out_hashchain.add(_hidl_vec_element);
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public void setHALInstrumentation() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256462420, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) throws RemoteException {
            return this.mRemote.linkToDeath(recipient, cookie);
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public void ping() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(256921159, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public DebugInfo getDebugInfo() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257049926, _hidl_request, _hidl_reply, 0);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
                DebugInfo _hidl_out_info = new DebugInfo();
                _hidl_out_info.readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public void notifySyspropsChanged() throws RemoteException {
            HwParcel _hidl_request = new HwParcel();
            _hidl_request.writeInterfaceToken(IBase.kInterfaceName);
            HwParcel _hidl_reply = new HwParcel();
            try {
                this.mRemote.transact(257120595, _hidl_request, _hidl_reply, 1);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) throws RemoteException {
            return this.mRemote.unlinkToDeath(recipient);
        }
    }

    /* loaded from: classes.dex */
    public static abstract class Stub extends HwBinder implements IMiFaceSession {
        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public IHwBinder asBinder() {
            return this;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<String> interfaceChain() {
            return new ArrayList<>(Arrays.asList(IMiFaceSession.kInterfaceName, IBase.kInterfaceName));
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public void debug(NativeHandle fd, ArrayList<String> options) {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final String interfaceDescriptor() {
            return IMiFaceSession.kInterfaceName;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final ArrayList<byte[]> getHashChain() {
            return new ArrayList<>(Arrays.asList(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, new byte[]{-20, Byte.MAX_VALUE, -41, -98, -48, 45, -6, -123, -68, 73, -108, 38, -83, -82, 62, -66, CommunicationUtil.RESPONSE_VENDOR_ONE_SHORT_REPORT_ID, -17, 5, CommunicationUtil.RESPONSE_VENDOR_ONE_LONG_REPORT_ID, -13, -51, CommunicationUtil.COMMAND_CUSTOM_DATA_KB, CommunicationUtil.RESPONSE_TYPE, 19, -109, CommunicationUtil.RESPONSE_VENDOR_ONE_LONG_REPORT_ID, -72, 59, CommunicationUtil.MCU_ADDRESS, -54, 76}));
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void setHALInstrumentation() {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean linkToDeath(IHwBinder.DeathRecipient recipient, long cookie) {
            return true;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void ping() {
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final DebugInfo getDebugInfo() {
            DebugInfo info = new DebugInfo();
            info.pid = HidlSupport.getPidIfSharable();
            info.ptr = 0L;
            info.arch = 0;
            return info;
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final void notifySyspropsChanged() {
            HwBinder.enableInstrumentation();
        }

        @Override // com.android.server.biometrics.sensors.face.IMiFaceSession, miui.android.services.internal.hidl.base.V1_0.IBase
        public final boolean unlinkToDeath(IHwBinder.DeathRecipient recipient) {
            return true;
        }

        public IHwInterface queryLocalInterface(String descriptor) {
            if (IMiFaceSession.kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws RemoteException {
            registerService(serviceName);
        }

        public String toString() {
            return interfaceDescriptor() + "@Stub";
        }

        public void onTransact(int _hidl_code, HwParcel _hidl_request, HwParcel _hidl_reply, int _hidl_flags) throws RemoteException {
            switch (_hidl_code) {
                case 1:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    long _hidl_out_status = generateChallenge();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_status);
                    _hidl_reply.send();
                    return;
                case 2:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    long challenge = _hidl_request.readInt64();
                    int _hidl_out_status2 = revokeChallenge(challenge);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status2);
                    _hidl_reply.send();
                    return;
                case 3:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    ArrayList<Byte> hat = _hidl_request.readInt8Vector();
                    ArrayList<Integer> features = _hidl_request.readInt32Vector();
                    NativeHandle previewSurface = _hidl_request.readNativeHandle();
                    int _hidl_out_status3 = enroll(hat, features, previewSurface);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status3);
                    _hidl_reply.send();
                    return;
                case 4:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    long operationId = _hidl_request.readInt64();
                    int _hidl_out_status4 = authenticate(operationId);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status4);
                    _hidl_reply.send();
                    return;
                case 5:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    int _hidl_out_status5 = enumerateEnrollments();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status5);
                    _hidl_reply.send();
                    return;
                case 6:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    ArrayList<Integer> enrollmentIds = _hidl_request.readInt32Vector();
                    int _hidl_out_status6 = removeEnrollments(enrollmentIds);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status6);
                    _hidl_reply.send();
                    return;
                case 7:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    int _hidl_out_status7 = getFeatures();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status7);
                    _hidl_reply.send();
                    return;
                case 8:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    ArrayList<Byte> hat2 = _hidl_request.readInt8Vector();
                    int feature = _hidl_request.readInt32();
                    boolean enabled = _hidl_request.readBool();
                    int _hidl_out_status8 = setFeature(hat2, feature, enabled);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status8);
                    _hidl_reply.send();
                    return;
                case 9:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    long _hidl_out_result = getAuthenticatorId();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt64(_hidl_out_result);
                    _hidl_reply.send();
                    return;
                case 10:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    int _hidl_out_status9 = invalidateAuthenticatorId();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status9);
                    _hidl_reply.send();
                    return;
                case 11:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    ArrayList<Byte> hat3 = _hidl_request.readInt8Vector();
                    int _hidl_out_status10 = resetLockout(hat3);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status10);
                    _hidl_reply.send();
                    return;
                case 12:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    int _hidl_out_status11 = close();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status11);
                    _hidl_reply.send();
                    return;
                case 13:
                    _hidl_request.enforceInterface(IMiFaceSession.kInterfaceName);
                    int _hidl_out_status12 = cancel();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeInt32(_hidl_out_status12);
                    _hidl_reply.send();
                    return;
                case 256067662:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    return;
                case 256131655:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    NativeHandle fd = _hidl_request.readNativeHandle();
                    ArrayList<String> options = _hidl_request.readStringVector();
                    debug(fd, options);
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 256136003:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    return;
                case 256398152:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ArrayList<byte[]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(0);
                    HwBlob _hidl_blob = new HwBlob(16);
                    int _hidl_vec_size = _hidl_out_hashchain.size();
                    _hidl_blob.putInt32(8L, _hidl_vec_size);
                    _hidl_blob.putBool(12L, false);
                    HwBlob childBlob = new HwBlob(_hidl_vec_size * 32);
                    for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; _hidl_index_0++) {
                        long _hidl_array_offset_1 = _hidl_index_0 * 32;
                        byte[] _hidl_array_item_1 = _hidl_out_hashchain.get(_hidl_index_0);
                        if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                            throw new IllegalArgumentException("Array element is not of the expected length");
                        }
                        childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                    }
                    _hidl_blob.putBlob(0L, childBlob);
                    _hidl_reply.writeBuffer(_hidl_blob);
                    _hidl_reply.send();
                    return;
                case 256462420:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    setHALInstrumentation();
                    return;
                case 256660548:
                default:
                    return;
                case 256921159:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    ping();
                    _hidl_reply.writeStatus(0);
                    _hidl_reply.send();
                    return;
                case 257049926:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(0);
                    _hidl_out_info.writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    return;
                case 257120595:
                    _hidl_request.enforceInterface(IBase.kInterfaceName);
                    notifySyspropsChanged();
                    return;
            }
        }
    }
}
