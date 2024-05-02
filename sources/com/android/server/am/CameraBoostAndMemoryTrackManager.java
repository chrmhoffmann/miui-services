package com.android.server.am;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import com.miui.analytics.ITrackBinder;
import java.util.List;
/* loaded from: classes.dex */
public class CameraBoostAndMemoryTrackManager {
    private static final String APP_ID = "31000000285";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String TAG = "ProcessManager";
    private Context mContext;
    private ITrackBinder mITrackBinder;
    private OneTrackListener mOneTrackListener;
    private ServiceConnection mServiceConnection;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OneTrackListener {
        void onConnect(ServiceConnection serviceConnection);
    }

    public CameraBoostAndMemoryTrackManager(Context context) {
        this.mContext = context;
        bindOneTrackService();
    }

    private void bindOneTrackService() {
        try {
            Intent intent = new Intent();
            intent.setClassName(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE, "com.miui.analytics.onetrack.TrackService");
            ServiceConnection serviceConnection = new ServiceConnection() { // from class: com.android.server.am.CameraBoostAndMemoryTrackManager.1
                @Override // android.content.ServiceConnection
                public void onServiceConnected(ComponentName name, IBinder service) {
                    CameraBoostAndMemoryTrackManager.this.mITrackBinder = ITrackBinder.Stub.asInterface(service);
                    if (CameraBoostAndMemoryTrackManager.this.mOneTrackListener != null) {
                        CameraBoostAndMemoryTrackManager.this.mOneTrackListener.onConnect(CameraBoostAndMemoryTrackManager.this.mServiceConnection);
                    }
                }

                @Override // android.content.ServiceConnection
                public void onServiceDisconnected(ComponentName name) {
                    CameraBoostAndMemoryTrackManager.this.mITrackBinder = null;
                }
            };
            this.mServiceConnection = serviceConnection;
            this.mContext.bindService(intent, serviceConnection, 1);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ProcessManager", "Bind Service Exception");
        }
    }

    public void track(List<String> dataList) {
        if (this.mITrackBinder == null) {
            Log.e("ProcessManager", "mITrackBinder == null");
        } else if (dataList == null || dataList.size() <= 0) {
            Log.e("ProcessManager", "dataList == null");
        } else {
            try {
                this.mITrackBinder.trackEvents(APP_ID, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE, dataList, 3);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ProcessManager", "Track Exception");
            }
        }
    }

    public void registListener(OneTrackListener oneTrackListener) {
        this.mOneTrackListener = oneTrackListener;
    }
}
