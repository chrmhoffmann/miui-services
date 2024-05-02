package com.android.server.wm;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.sizecompat.AspectRatioInfo;
import android.sizecompat.MiuiSizeCompatManager;
import android.util.Log;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.miui.analytics.ITrackBinder;
import java.util.Map;
import java.util.function.BiConsumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiSizeCompatJob extends JobService {
    private static final String APP_ID = "31000000779";
    private static final String EVENT_NAME = "status";
    private static final String EVENT_STATUS_KEY = "app_display_status";
    private static final String EVENT_STATUS_NAME_KEY = "app_display_name";
    private static final String EVENT_STATUS_PACKAGE_KEY = "app_package_name";
    private static final String EVENT_STATUS_STYLE_KEY = "app_display_style";
    private static final String EVENT_STYLE_EMBEDED_VALUE = "平行窗口";
    private static final String SERVER_CLASS_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVER_PKG_NAME = "com.miui.analytics";
    private static final int SIZE_COMPAT_JOB_ID = 33525;
    private static final String TAG = "MiuiSizeCompatJob";
    private Handler mH;
    private ITrackBinder mITrackBinder;
    private static final String APP_PKG = "android";
    private static final ComponentName mCName = new ComponentName(APP_PKG, MiuiSizeCompatJob.class.getName());
    private Object mTrackLock = new Object();
    private ServiceConnection mServiceConnection = new ServiceConnection() { // from class: com.android.server.wm.MiuiSizeCompatJob.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (MiuiSizeCompatJob.this.mTrackLock) {
                MiuiSizeCompatJob.this.mITrackBinder = ITrackBinder.Stub.asInterface(service);
                if (MiuiSizeCompatJob.this.mITrackBinder != null) {
                    try {
                        MiuiSizeCompatJob.this.mITrackBinder.asBinder().linkToDeath(MiuiSizeCompatJob.this.mDeathRecipient, 0);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(MiuiSizeCompatJob.TAG, "Bind OneTrack service success.");
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.d(MiuiSizeCompatJob.TAG, "OneTrack service disconnected.");
            synchronized (MiuiSizeCompatJob.this.mTrackLock) {
                MiuiSizeCompatJob.this.mITrackBinder = null;
            }
        }
    };
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.wm.MiuiSizeCompatJob.2
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.d(MiuiSizeCompatJob.TAG, "DeathRecipient ");
            synchronized (MiuiSizeCompatJob.this.mTrackLock) {
                if (MiuiSizeCompatJob.this.mITrackBinder == null) {
                    return;
                }
                MiuiSizeCompatJob.this.mITrackBinder.asBinder().unlinkToDeath(MiuiSizeCompatJob.this.mDeathRecipient, 0);
                MiuiSizeCompatJob.this.mITrackBinder = null;
                MiuiSizeCompatJob.this.mH.removeMessages(2);
                MiuiSizeCompatJob.this.mH.sendEmptyMessageDelayed(2, 300L);
            }
        }
    };

    @Override // android.app.Service
    public void onCreate() {
        Log.d(TAG, "onCreate");
        this.mH = new H(MiuiBgThread.get().getLooper());
        bindOneTrackService();
    }

    public static void sheduleJob(Context context) {
        JobScheduler js = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo jobInfo = new JobInfo.Builder(SIZE_COMPAT_JOB_ID, mCName).setRequiredNetworkType(2).setPeriodic(86400000L).build();
        js.schedule(jobInfo);
    }

    /* loaded from: classes.dex */
    private class H extends Handler {
        private static final int MSG_BIND_ONE_TRACK = 2;
        private static final int MSG_TRACK_EVENT = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiSizeCompatJob.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj instanceof JobParameters) {
                        MiuiSizeCompatJob.this.trackEvent((JobParameters) msg.obj);
                        return;
                    } else {
                        Log.e(MiuiSizeCompatJob.TAG, "Error obj ,not job params");
                        return;
                    }
                case 2:
                    MiuiSizeCompatJob.this.bindOneTrackService();
                    return;
                default:
                    Slog.e(MiuiSizeCompatJob.TAG, "Error message what!");
                    return;
            }
        }
    }

    public void bindOneTrackService() {
        synchronized (this.mTrackLock) {
            if (this.mITrackBinder != null) {
                Slog.d(TAG, "Already bound.");
                return;
            }
            try {
                Intent intent = new Intent();
                intent.setClassName("com.miui.analytics", SERVER_CLASS_NAME);
                bindServiceAsUser(intent, this.mServiceConnection, 1, UserHandle.CURRENT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unBindOneTrackService() {
        synchronized (this.mTrackLock) {
            ITrackBinder iTrackBinder = this.mITrackBinder;
            if (iTrackBinder != null) {
                iTrackBinder.asBinder().unlinkToDeath(this.mDeathRecipient, 0);
                this.mITrackBinder = null;
                Slog.d(TAG, "unbind OneTrack Service success.");
            } else {
                Slog.d(TAG, "unbind OneTrack Service fail.");
            }
            ServiceConnection serviceConnection = this.mServiceConnection;
            if (serviceConnection != null) {
                unbindService(serviceConnection);
                this.mServiceConnection = null;
            }
        }
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        if (params.getJobId() == SIZE_COMPAT_JOB_ID) {
            Log.d(TAG, "Start job!");
            this.mH.removeMessages(1);
            Handler handler = this.mH;
            handler.sendMessageDelayed(handler.obtainMessage(1, params), 1000L);
            return true;
        }
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        if (params.getJobId() == SIZE_COMPAT_JOB_ID) {
            Log.d(TAG, "Stop job!");
            return false;
        }
        return false;
    }

    public void trackEvent(JobParameters parameters) {
        String str;
        String str2;
        synchronized (this.mTrackLock) {
            long start = SystemClock.uptimeMillis();
            try {
                Map<String, AspectRatioInfo> map = MiuiSizeCompatManager.getMiuiSizeCompatInstalledApps();
                final Map<String, Boolean> embeddedApps = getEmbeddedApps();
                if (this.mITrackBinder != null && map != null) {
                    JSONObject object = new JSONObject();
                    final JSONArray array = new JSONArray();
                    map.forEach(new BiConsumer<String, AspectRatioInfo>() { // from class: com.android.server.wm.MiuiSizeCompatJob.3
                        public void accept(String pkgName, AspectRatioInfo aspectRatioInfo) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("app_package_name", pkgName);
                                jsonObject.put(MiuiSizeCompatJob.EVENT_STATUS_NAME_KEY, aspectRatioInfo.mApplicationName);
                                Map map2 = embeddedApps;
                                if (map2 != null && map2.containsKey(pkgName) && aspectRatioInfo.mAspectRatio == MiuiFreeformPinManagerService.EDGE_AREA) {
                                    jsonObject.put(MiuiSizeCompatJob.EVENT_STATUS_STYLE_KEY, MiuiSizeCompatJob.EVENT_STYLE_EMBEDED_VALUE);
                                } else {
                                    jsonObject.put(MiuiSizeCompatJob.EVENT_STATUS_STYLE_KEY, aspectRatioInfo.getRatioStr());
                                }
                                array.put(jsonObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    object.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "status");
                    object.put(EVENT_STATUS_KEY, array);
                    this.mITrackBinder.trackEvent(APP_ID, APP_PKG, object.toString(), 0);
                    long took = SystemClock.uptimeMillis() - start;
                    if (MiuiSizeCompatService.DEBUG) {
                        Log.d(TAG, array.toString());
                    }
                    Log.d(TAG, "Track " + array.length() + " events took " + took + " ms.");
                }
                jobFinished(parameters, false);
                str2 = TAG;
                str = "Job finished";
            } catch (Exception e) {
                e.printStackTrace();
                jobFinished(parameters, false);
                str2 = TAG;
                str = "Job finished";
            }
            Log.d(str2, str);
        }
    }

    private Map<String, Boolean> getEmbeddedApps() {
        return null;
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unBindOneTrackService();
    }
}
