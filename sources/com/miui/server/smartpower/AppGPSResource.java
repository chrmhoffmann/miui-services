package com.miui.server.smartpower;

import android.content.Context;
import android.location.ILocationListener;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AppGPSResource extends AppPowerResource {
    private final ArrayMap<Integer, GpsRecord> mGpsRecordMap = new ArrayMap<>();

    public AppGPSResource(Context context, Looper looper) {
        this.mType = 3;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList getActiveUids() {
        ArrayList arrayList;
        synchronized (this.mGpsRecordMap) {
            arrayList = new ArrayList(this.mGpsRecordMap.keySet());
        }
        return arrayList;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mGpsRecordMap) {
            for (int i = 0; i < this.mGpsRecordMap.size(); i++) {
                GpsRecord record = this.mGpsRecordMap.valueAt(i);
                if (record != null && record.mUid == uid && record.isActive()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        synchronized (this.mGpsRecordMap) {
            GpsRecord record = this.mGpsRecordMap.get(Integer.valueOf(pid));
            if (record != null) {
                return record.isActive();
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int pid) {
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int pid) {
    }

    public void onAquireLocation(int uid, int pid, ILocationListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mGpsRecordMap) {
            GpsRecord gpsRecord = this.mGpsRecordMap.get(Integer.valueOf(pid));
            if (gpsRecord == null) {
                gpsRecord = new GpsRecord(uid, pid);
                this.mGpsRecordMap.put(Integer.valueOf(pid), gpsRecord);
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "gps u:" + uid + " p:" + pid + " s:true");
                reportResourceStatus(uid, pid, true, 64);
            }
            gpsRecord.onAquireLocation(listener);
        }
    }

    public void onReleaseLocation(int uid, int pid, ILocationListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (this.mGpsRecordMap) {
            GpsRecord gpsRecord = this.mGpsRecordMap.get(Integer.valueOf(pid));
            if (gpsRecord == null) {
                return;
            }
            gpsRecord.onReleaseLocation(listener);
            if (!gpsRecord.isActive()) {
                this.mGpsRecordMap.remove(Integer.valueOf(pid));
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "gps u:" + uid + " p:" + pid + " s:false");
                reportResourceStatus(uid, pid, false, 64);
            }
        }
    }

    /* loaded from: classes.dex */
    public class GpsRecord {
        private ArraySet<ILocationListener> mLocationListeners = new ArraySet<>();
        private final int mPid;
        private final int mUid;

        GpsRecord(int uid, int pid) {
            AppGPSResource.this = this$0;
            this.mUid = uid;
            this.mPid = pid;
        }

        boolean isActive() {
            return this.mLocationListeners.size() > 0;
        }

        int getPid() {
            return this.mPid;
        }

        int getUid() {
            return this.mUid;
        }

        void onAquireLocation(ILocationListener listener) {
            for (int i = 0; i < this.mLocationListeners.size(); i++) {
                ILocationListener item = this.mLocationListeners.valueAt(i);
                if (item.asBinder().equals(listener.asBinder())) {
                    return;
                }
            }
            this.mLocationListeners.add(listener);
        }

        void onReleaseLocation(ILocationListener listener) {
            for (int i = 0; i < this.mLocationListeners.size(); i++) {
                ILocationListener item = this.mLocationListeners.valueAt(i);
                if (item.asBinder().equals(listener.asBinder())) {
                    this.mLocationListeners.remove(item);
                }
            }
        }
    }
}
