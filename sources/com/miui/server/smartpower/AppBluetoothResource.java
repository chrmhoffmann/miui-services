package com.miui.server.smartpower;

import android.content.Context;
import android.os.Looper;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.miui.server.smartpower.AppPowerResource;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AppBluetoothResource extends AppPowerResource {
    private final SparseArray<BleRecord> mActivePidsMap = new SparseArray<>();

    public AppBluetoothResource(Context context, Looper looper) {
        this.mType = 5;
    }

    private BleRecord getBleRecord(int pid) {
        BleRecord bleRecord;
        synchronized (this.mActivePidsMap) {
            bleRecord = this.mActivePidsMap.get(pid);
        }
        return bleRecord;
    }

    private BleRecord getOrCreateBleRecord(int uid, int pid) {
        BleRecord record;
        synchronized (this.mActivePidsMap) {
            record = this.mActivePidsMap.get(pid);
            if (record == null) {
                record = new BleRecord(uid, pid);
                this.mActivePidsMap.put(pid, record);
            }
        }
        return record;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList<Integer> getActiveUids() {
        return null;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                BleRecord record = this.mActivePidsMap.valueAt(i);
                if (record != null && record.mOwnerUid == uid && record.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        BleRecord record = getBleRecord(pid);
        if (record != null) {
            return record.isActive();
        }
        return false;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void registerCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        super.registerCallback(callback, uid);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void unRegisterCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid) {
        super.unRegisterCallback(callback, uid);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void registerCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid, int pid) {
        super.registerCallback(callback, uid, pid);
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void unRegisterCallback(AppPowerResource.IAppPowerResourceCallback callback, int uid, int pid) {
        synchronized (this.mActivePidsMap) {
            if (this.mActivePidsMap.get(pid) != null) {
                this.mActivePidsMap.remove(pid);
            }
        }
        super.unRegisterCallback(callback, uid, pid);
    }

    public void onBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, int flag) {
        if (DEBUG) {
            Slog.d(AppPowerResourceManager.TAG, "bluttooth: connect=" + isConnect + " bleType=" + bleType + " uid=" + uid + " pid=" + pid + " bleType=" + bleType + " flag=" + flag);
        }
        if (isConnect) {
            getOrCreateBleRecord(uid, pid).onBluetoothConnect(bleType, flag);
            return;
        }
        BleRecord record = getBleRecord(pid);
        if (record != null) {
            record.onBluetoothDisconnect(bleType, flag);
            if (!record.isActive()) {
                synchronized (this.mActivePidsMap) {
                    this.mActivePidsMap.remove(pid);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class BleRecord {
        int mOwnerPid;
        int mOwnerUid;
        private final SparseArray<ProfileRecord> mProfileRecords = new SparseArray<>();

        BleRecord(int uid, int pid) {
            AppBluetoothResource.this = r1;
            this.mOwnerUid = uid;
            this.mOwnerPid = pid;
        }

        void onBluetoothConnect(int bleType, int flag) {
            ProfileRecord profileRecord;
            synchronized (this.mProfileRecords) {
                profileRecord = this.mProfileRecords.get(bleType);
                if (profileRecord == null) {
                    profileRecord = new ProfileRecord(bleType);
                    this.mProfileRecords.put(bleType, profileRecord);
                }
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "bluetooth u:" + this.mOwnerUid + " p:" + this.mOwnerPid + " s:true");
                AppBluetoothResource.this.reportResourceStatus(this.mOwnerUid, this.mOwnerPid, true, 0);
            }
            profileRecord.bluetoothConnect(flag);
        }

        void onBluetoothDisconnect(int bleType, int flag) {
            synchronized (this.mProfileRecords) {
                ProfileRecord profileRecord = this.mProfileRecords.get(bleType);
                if (profileRecord != null) {
                    profileRecord.bluetoothDisconnect(flag);
                    if (!profileRecord.isActive()) {
                        this.mProfileRecords.remove(bleType);
                        EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "bluetooth u:" + this.mOwnerUid + " p:" + this.mOwnerPid + " s:false");
                        AppBluetoothResource.this.reportResourceStatus(this.mOwnerUid, this.mOwnerPid, false, 0);
                    }
                }
            }
        }

        boolean isActive() {
            return this.mProfileRecords.size() > 0;
        }

        /* loaded from: classes.dex */
        public class ProfileRecord {
            int mBleType;
            private final ArraySet<Integer> mFlags = new ArraySet<>();

            ProfileRecord(int bleType) {
                BleRecord.this = this$1;
                this.mBleType = bleType;
            }

            void bluetoothConnect(int flag) {
                this.mFlags.add(Integer.valueOf(flag));
            }

            void bluetoothDisconnect(int flag) {
                this.mFlags.remove(Integer.valueOf(flag));
            }

            boolean isActive() {
                return this.mFlags.size() > 0;
            }
        }
    }
}
