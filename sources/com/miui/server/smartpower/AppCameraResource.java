package com.miui.server.smartpower;

import android.content.Context;
import android.os.Looper;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AppCameraResource extends AppPowerResource {
    private final SparseArray<CameraRecord> mActivePidsMap = new SparseArray<>();

    public AppCameraResource(Context context, Looper looper) {
        this.mType = 6;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public ArrayList<Integer> getActiveUids() {
        return null;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid) {
        synchronized (this.mActivePidsMap) {
            for (int i = 0; i < this.mActivePidsMap.size(); i++) {
                CameraRecord record = this.mActivePidsMap.valueAt(i);
                if (record != null && record.mCallerUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public boolean isAppResourceActive(int uid, int pid) {
        boolean contains;
        synchronized (this.mActivePidsMap) {
            contains = this.mActivePidsMap.contains(pid);
        }
        return contains;
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void releaseAppPowerResource(int uid) {
    }

    @Override // com.miui.server.smartpower.AppPowerResource
    public void resumeAppPowerResource(int uid) {
    }

    public void notifyCameraForegroundState(String cameraId, boolean isForeground, String caller, int callerUid, int callerPid) {
        CameraRecord record;
        synchronized (this.mActivePidsMap) {
            record = this.mActivePidsMap.get(callerPid);
            if (isForeground) {
                if (record == null) {
                    record = new CameraRecord(caller, callerUid, callerPid);
                    this.mActivePidsMap.put(callerPid, record);
                }
            } else if (record != null) {
                synchronized (this.mActivePidsMap) {
                    this.mActivePidsMap.remove(callerPid);
                }
            }
        }
        if (record != null) {
            record.notifyCameraForegroundState(cameraId, isForeground);
        }
    }

    /* loaded from: classes.dex */
    public class CameraRecord {
        private String mCallerPackage;
        private int mCallerPid;
        private int mCallerUid;

        public CameraRecord(String caller, int callerUid, int callerPid) {
            AppCameraResource.this = r1;
            this.mCallerUid = callerUid;
            this.mCallerPid = callerPid;
            this.mCallerPackage = caller;
        }

        public void notifyCameraForegroundState(String cameraId, boolean isForeground) {
            if (AppPowerResource.DEBUG) {
                Slog.d(AppPowerResourceManager.TAG, "camera: active=" + isForeground + " cameraId=" + cameraId + " pkg=" + this.mCallerPackage + " uid=" + this.mCallerUid + " pid=" + this.mCallerPid);
            }
            AppCameraResource.this.reportResourceStatus(this.mCallerUid, this.mCallerPid, isForeground, 32);
            AppCameraResource.this.reportResourceStatus(this.mCallerUid, isForeground, 32);
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "camera u:" + this.mCallerUid + " p:" + this.mCallerPid + " s:" + isForeground);
        }
    }
}
