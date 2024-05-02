package com.android.server.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
/* loaded from: classes.dex */
public class CameraTrackInjector implements CameraTrackExt {
    private static final String APP_ID = "31000000285";
    private static final String CAMERAUSAGE_API = "attr_camerausage_api";
    private static final String CAMERAUSAGE_CLIENT_NAME = "attr_camerausage_client_name";
    private static final String CAMERAUSAGE_DURATION = "attr_camerausage_duration";
    private static final String CAMERAUSAGE_EVENT_NAME = "key_camerausage";
    private static final String CAMERAUSAGE_ID = "attr_camerausage_id";
    private static final int CAMERA_STATE_CLOSED = 1;
    private static final int CAMERA_STATE_OPEN = 0;
    private static final String PACKAGE = "com.android.camera";
    private static final String TAG = CameraTrackInjector.class.getSimpleName();
    private ArrayMap<String, CameraUsageBean> mActiveCameraUsage = new ArrayMap<>();
    private Context mContext;

    /* loaded from: classes.dex */
    public static class CameraUsageBean {
        public int mCameraAPI;
        public String mCameraId;
        public String mClientName;
        private long mDurationOrStartTimeMs = SystemClock.elapsedRealtime();
        private boolean mCompleted = false;

        public CameraUsageBean(String clientName, String cameraId, int apiLevel) {
            this.mCameraId = cameraId;
            this.mClientName = clientName;
            this.mCameraAPI = apiLevel;
        }

        public void markCompleted() {
            if (this.mCompleted) {
                return;
            }
            this.mCompleted = true;
            this.mDurationOrStartTimeMs = SystemClock.elapsedRealtime() - this.mDurationOrStartTimeMs;
        }

        public long getDuration() {
            if (this.mCompleted) {
                return this.mDurationOrStartTimeMs / 1000;
            }
            return 0L;
        }

        public String toString() {
            return "CameraUsageBean(clientName=" + this.mClientName + ",CameraAPI=" + this.mCameraAPI + ",cameraId=" + this.mCameraId + ",durationOrStartTimeS=" + getDuration() + ")";
        }
    }

    public void trackCameraState(Context context, String cameraId, int newCameraState, String clientName, int apiLevel) {
        if (clientName != null && clientName.equals("com.android.camera")) {
            return;
        }
        this.mContext = context;
        switch (newCameraState) {
            case 0:
                if (this.mActiveCameraUsage.size() > 100) {
                    this.mActiveCameraUsage.clear();
                }
                CameraUsageBean oldBean = this.mActiveCameraUsage.put(clientName + "_" + cameraId, new CameraUsageBean(clientName, cameraId, apiLevel));
                if (oldBean != null) {
                    oldBean.markCompleted();
                    trackCamera(oldBean);
                    return;
                }
                return;
            case 1:
                CameraUsageBean cameraUsageBean = this.mActiveCameraUsage.remove(clientName + "_" + cameraId);
                if (cameraUsageBean != null) {
                    cameraUsageBean.markCompleted();
                    trackCamera(cameraUsageBean);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void trackCamera(CameraUsageBean bean) {
        if (this.mContext == null) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            try {
                Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
                intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "com.android.camera");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, CAMERAUSAGE_EVENT_NAME);
                Bundle params = new Bundle();
                params.putString(CAMERAUSAGE_CLIENT_NAME, bean.mClientName);
                params.putString(CAMERAUSAGE_ID, bean.mCameraId);
                params.putLong(CAMERAUSAGE_DURATION, bean.getDuration());
                params.putInt(CAMERAUSAGE_API, bean.mCameraAPI);
                intent.putExtras(params);
                this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (Exception e) {
                Log.e(TAG, "start onetrack service : " + e.toString());
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }
}
