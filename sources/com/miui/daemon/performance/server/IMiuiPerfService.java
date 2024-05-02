package com.miui.daemon.performance.server;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.statistics.E2EScenario;
import android.os.statistics.E2EScenarioPayload;
import android.os.statistics.E2EScenarioSettings;
import java.util.List;
/* loaded from: classes.dex */
public interface IMiuiPerfService extends IInterface {
    void abortMatchingScenario(E2EScenario e2EScenario, String str, long j, int i, int i2, String str2, String str3) throws RemoteException;

    void abortSpecificScenario(Bundle bundle, long j, int i, int i2, String str, String str2) throws RemoteException;

    Bundle beginScenario(E2EScenario e2EScenario, E2EScenarioSettings e2EScenarioSettings, String str, E2EScenarioPayload e2EScenarioPayload, long j, int i, int i2, String str2, String str3, boolean z) throws RemoteException;

    void dump(String[] strArr) throws RemoteException;

    void finishMatchingScenario(E2EScenario e2EScenario, String str, E2EScenarioPayload e2EScenarioPayload, long j, int i, int i2, String str2, String str3) throws RemoteException;

    void finishSpecificScenario(Bundle bundle, E2EScenarioPayload e2EScenarioPayload, long j, int i, int i2, String str, String str2) throws RemoteException;

    ParcelFileDescriptor getPerfEventSocketFd() throws RemoteException;

    void markPerceptibleJank(Bundle bundle) throws RemoteException;

    void reportActivityLaunchRecords(List<Bundle> list) throws RemoteException;

    void reportExcessiveCpuUsageRecords(List<Bundle> list) throws RemoteException;

    void reportFocusChanged(String str, String str2, String str3, String str4) throws RemoteException;

    void reportMiSpeedRecord(Bundle bundle) throws RemoteException;

    void reportNotificationClick(String str, Intent intent, long j) throws RemoteException;

    void reportProcessCleanEvent(Bundle bundle) throws RemoteException;

    void reportPssRecord(String str, String str2, long j, String str3, int i) throws RemoteException;

    void setSchedFgPid(int i) throws RemoteException;

    /* loaded from: classes.dex */
    public static abstract class Stub extends Binder implements IMiuiPerfService {
        private static final String DESCRIPTOR = "com.miui.daemon.performance.server.IMiuiPerfService";
        static final int TRANSACTION_abortMatchingScenario = 7;
        static final int TRANSACTION_abortSpecificScenario = 8;
        static final int TRANSACTION_beginScenario = 6;
        static final int TRANSACTION_dump = 3;
        static final int TRANSACTION_finishMatchingScenario = 9;
        static final int TRANSACTION_finishSpecificScenario = 10;
        static final int TRANSACTION_getPerfEventSocketFd = 5;
        static final int TRANSACTION_markPerceptibleJank = 1;
        static final int TRANSACTION_reportActivityLaunchRecords = 2;
        static final int TRANSACTION_reportExcessiveCpuUsageRecords = 11;
        static final int TRANSACTION_reportFocusChanged = 16;
        static final int TRANSACTION_reportMiSpeedRecord = 15;
        static final int TRANSACTION_reportNotificationClick = 12;
        static final int TRANSACTION_reportProcessCleanEvent = 13;
        static final int TRANSACTION_reportPssRecord = 14;
        static final int TRANSACTION_setSchedFgPid = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMiuiPerfService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IMiuiPerfService)) {
                return (IMiuiPerfService) iin;
            }
            return new Proxy(obj);
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Intent intent;
            Bundle bundle = null;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    }
                    Bundle bundle2 = bundle;
                    markPerceptibleJank(bundle2);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    List<Bundle> launchRecords = data.createTypedArrayList(Bundle.CREATOR);
                    reportActivityLaunchRecords(launchRecords);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String[] args = data.createStringArray();
                    dump(args);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int pid = data.readInt();
                    setSchedFgPid(pid);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    ParcelFileDescriptor _result = getPerfEventSocketFd();
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    E2EScenario scenario = (E2EScenario) data.readParcelable(null);
                    E2EScenarioSettings settings = (E2EScenarioSettings) data.readParcelable(null);
                    String tag = data.readString();
                    E2EScenarioPayload payload = (E2EScenarioPayload) data.readParcelable(null);
                    long uptimeMillis = data.readLong();
                    int pid2 = data.readInt();
                    int tid = data.readInt();
                    String processName = data.readString();
                    String packageName = data.readString();
                    boolean needResultBundle = data.readInt() == 1;
                    Bundle _result2 = beginScenario(scenario, settings, tag, payload, uptimeMillis, pid2, tid, processName, packageName, needResultBundle);
                    reply.writeNoException();
                    reply.writeBundle(_result2);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    E2EScenario scenario2 = (E2EScenario) data.readParcelable(null);
                    String tag2 = data.readString();
                    long uptimeMillis2 = data.readLong();
                    int pid3 = data.readInt();
                    int tid2 = data.readInt();
                    String processName2 = data.readString();
                    String packageName2 = data.readString();
                    abortMatchingScenario(scenario2, tag2, uptimeMillis2, pid3, tid2, processName2, packageName2);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    Bundle scenarioBundle = data.readBundle();
                    long uptimeMillis3 = data.readLong();
                    int pid4 = data.readInt();
                    int tid3 = data.readInt();
                    String processName3 = data.readString();
                    String packageName3 = data.readString();
                    abortSpecificScenario(scenarioBundle, uptimeMillis3, pid4, tid3, processName3, packageName3);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    E2EScenario scenario3 = (E2EScenario) data.readParcelable(null);
                    String tag3 = data.readString();
                    E2EScenarioPayload payload2 = (E2EScenarioPayload) data.readParcelable(null);
                    long uptimeMillis4 = data.readLong();
                    int pid5 = data.readInt();
                    int tid4 = data.readInt();
                    String processName4 = data.readString();
                    String packageName4 = data.readString();
                    finishMatchingScenario(scenario3, tag3, payload2, uptimeMillis4, pid5, tid4, processName4, packageName4);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    Bundle scenarioBundle2 = data.readBundle();
                    E2EScenarioPayload payload3 = (E2EScenarioPayload) data.readParcelable(null);
                    long uptimeMillis5 = data.readLong();
                    int pid6 = data.readInt();
                    int tid5 = data.readInt();
                    String processName5 = data.readString();
                    String packageName5 = data.readString();
                    finishSpecificScenario(scenarioBundle2, payload3, uptimeMillis5, pid6, tid5, processName5, packageName5);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    List<Bundle> records = data.createTypedArrayList(Bundle.CREATOR);
                    reportExcessiveCpuUsageRecords(records);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String postPackage = data.readString();
                    if (data.readInt() != 0) {
                        intent = (Intent) Intent.CREATOR.createFromParcel(data);
                    } else {
                        intent = null;
                    }
                    long uptimeMillis6 = data.readLong();
                    reportNotificationClick(postPackage, intent, uptimeMillis6);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bundle = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    }
                    Bundle bundle3 = bundle;
                    reportProcessCleanEvent(bundle3);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    String processName6 = data.readString();
                    String packageName6 = data.readString();
                    long pss = data.readLong();
                    String versionName = data.readString();
                    int versionCode = data.readInt();
                    reportPssRecord(processName6, packageName6, pss, versionName, versionCode);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    Bundle bundle4 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                    reportMiSpeedRecord(bundle4);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    String currentPkg = data.readString();
                    String newPkg = data.readString();
                    String currentWindow = data.readString();
                    String newWindow = data.readString();
                    reportFocusChanged(currentPkg, newPkg, currentWindow, newWindow);
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
        public static class Proxy implements IMiuiPerfService {
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

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void markPerceptibleJank(Bundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(1, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportActivityLaunchRecords(List<Bundle> launchRecords) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                _data.writeTypedList(launchRecords);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(2, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void dump(String[] args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                _data.writeStringArray(args);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(3, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void setSchedFgPid(int pid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                _data.writeInt(pid);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(4, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public ParcelFileDescriptor getPerfEventSocketFd() throws RemoteException {
                ParcelFileDescriptor _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public Bundle beginScenario(E2EScenario scenario, E2EScenarioSettings settings, String tag, E2EScenarioPayload payload, long uptimeMillis, int pid, int tid, String processName, String packageName, boolean needResultBundle) throws RemoteException {
                Throwable th;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                } catch (Throwable th2) {
                    th = th2;
                }
                try {
                    _data.writeParcelable(scenario, 0);
                    try {
                        _data.writeParcelable(settings, 0);
                        try {
                            _data.writeString(tag);
                            try {
                                _data.writeParcelable(payload, 0);
                            } catch (Throwable th3) {
                                th = th3;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(uptimeMillis);
                        try {
                            _data.writeInt(pid);
                            try {
                                _data.writeInt(tid);
                                try {
                                    _data.writeString(processName);
                                    try {
                                        _data.writeString(packageName);
                                        _data.writeInt(needResultBundle ? 1 : 0);
                                        try {
                                            this.mRemote.transact(6, _data, _reply, 0);
                                            _reply.readException();
                                            Bundle _result = _reply.readBundle();
                                            _reply.recycle();
                                            _data.recycle();
                                            return _result;
                                        } catch (Throwable th6) {
                                            th = th6;
                                            _reply.recycle();
                                            _data.recycle();
                                            throw th;
                                        }
                                    } catch (Throwable th7) {
                                        th = th7;
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th11) {
                        th = th11;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th12) {
                    th = th12;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void abortMatchingScenario(E2EScenario scenario, String tag, long uptimeMillis, int pid, int tid, String processName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeParcelable(scenario, 0);
                    _data.writeString(tag);
                    _data.writeLong(uptimeMillis);
                    _data.writeInt(pid);
                    _data.writeInt(tid);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void abortSpecificScenario(Bundle scenarioBundle, long uptimeMillis, int pid, int tid, String processName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBundle(scenarioBundle);
                    _data.writeLong(uptimeMillis);
                    _data.writeInt(pid);
                    _data.writeInt(tid);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void finishMatchingScenario(E2EScenario scenario, String tag, E2EScenarioPayload payload, long uptimeMillis, int pid, int tid, String processName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeParcelable(scenario, 0);
                    _data.writeString(tag);
                    _data.writeParcelable(payload, 0);
                    _data.writeLong(uptimeMillis);
                    _data.writeInt(pid);
                    _data.writeInt(tid);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void finishSpecificScenario(Bundle scenarioBundle, E2EScenarioPayload payload, long uptimeMillis, int pid, int tid, String processName, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBundle(scenarioBundle);
                    _data.writeParcelable(payload, 0);
                    _data.writeLong(uptimeMillis);
                    _data.writeInt(pid);
                    _data.writeInt(tid);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportExcessiveCpuUsageRecords(List<Bundle> records) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.writeInterfaceToken(Stub.DESCRIPTOR);
                _data.writeTypedList(records);
                Parcel _reply = Parcel.obtain();
                try {
                    this.mRemote.transact(11, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportNotificationClick(String postPackage, Intent intent, long uptimeMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(postPackage);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(uptimeMillis);
                    this.mRemote.transact(12, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportProcessCleanEvent(Bundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(13, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportPssRecord(String processName, String packageName, long pss, String versionName, int versionCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(processName);
                    _data.writeString(packageName);
                    _data.writeLong(pss);
                    _data.writeString(versionName);
                    _data.writeInt(versionCode);
                    this.mRemote.transact(14, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportFocusChanged(String currentPkg, String newPkg, String currentWindow, String newWindow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(currentPkg);
                    _data.writeString(newPkg);
                    _data.writeString(currentWindow);
                    _data.writeString(newWindow);
                    this.mRemote.transact(16, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override // com.miui.daemon.performance.server.IMiuiPerfService
            public void reportMiSpeedRecord(Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeBundle(data);
                    this.mRemote.transact(15, _data, _reply, 1);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
