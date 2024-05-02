package com.android.server.location.gnss.datacollect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import com.miui.analytics.ITrackBinder;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class GnssOneTrackManager {
    private static final String APP_ID = "2882303761518758754";
    private static final String CALL_INIT_FIRST = "please check whether call the init method first!";
    private static final String CAN_NOT_RUN_IN_MAINTHREAD = "Can not run in main thread!";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String ONETRACK_TRACK_EXCEPTION = "OneTrack Track Exception";
    private static final String ONETRACK_TRACK_SUCCESS = "OneTrack Track Success";
    private static final String PKG_NAME = "com.miui.analytics";
    private static final String SERVER_CLASS_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String TAG = "GnssOneTrackManager";
    private static volatile GnssOneTrackManager mGnssOneTrackManager;
    private boolean isServiceBind;
    private Context mBindContext;
    private ITrackBinder mITrackBinder;
    private final boolean D = SystemProperties.getBoolean("persist.sys.gnss_dc.test", false);
    private final Intent intent = new Intent().setClassName("com.miui.analytics", SERVER_CLASS_NAME);
    private final ServiceConnection conn = new ServiceConnection() { // from class: com.android.server.location.gnss.datacollect.GnssOneTrackManager.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(GnssOneTrackManager.TAG, "BindOneTrackService Success");
            synchronized (GnssOneTrackManager.class) {
                GnssOneTrackManager.this.mITrackBinder = ITrackBinder.Stub.asInterface(service);
                GnssOneTrackManager.class.notifyAll();
                GnssOneTrackManager.this.isServiceBind = true;
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            Log.i(GnssOneTrackManager.TAG, "Onetrack Service Disconnected");
            synchronized (GnssOneTrackManager.class) {
                GnssOneTrackManager.this.mITrackBinder = null;
                GnssOneTrackManager.this.mBindContext.unbindService(GnssOneTrackManager.this.conn);
                GnssOneTrackManager.this.isServiceBind = false;
            }
        }
    };

    private GnssOneTrackManager() {
    }

    public static GnssOneTrackManager getInstance() {
        if (mGnssOneTrackManager == null) {
            synchronized (GnssOneTrackManager.class) {
                if (mGnssOneTrackManager == null) {
                    mGnssOneTrackManager = new GnssOneTrackManager();
                }
            }
        }
        return mGnssOneTrackManager;
    }

    public void init(Context context) {
        if (context == null) {
            throw new RuntimeException("Init Context == null");
        }
        synchronized (GnssOneTrackManager.class) {
            Context context2 = this.mBindContext;
            if (context != context2 && this.isServiceBind) {
                this.mITrackBinder = null;
                context2.unbindService(this.conn);
            }
            this.mBindContext = context;
        }
    }

    public void track(JSONObject jsonData) throws RemoteException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new RuntimeException(CAN_NOT_RUN_IN_MAINTHREAD);
        }
        synchronized (GnssOneTrackManager.class) {
            if (this.mBindContext == null) {
                Log.e(TAG, CALL_INIT_FIRST);
            } else if (jsonData == null) {
                if (this.D) {
                    Log.d(TAG, "jsonData == null");
                }
            } else if (this.mITrackBinder != null) {
                if (this.D) {
                    Log.d(TAG, "json data call trackEvent");
                }
                this.mITrackBinder.trackEvent(APP_ID, "com.miui.analytics", jsonData.toString(), 3);
                Log.i(TAG, ONETRACK_TRACK_SUCCESS);
            } else {
                if (this.D) {
                    Log.d(TAG, "json data No TrackBinder, begin to bindService");
                }
                boolean bindResult = this.mBindContext.bindService(this.intent, this.conn, 1);
                try {
                    GnssOneTrackManager.class.wait(1000L);
                } catch (InterruptedException e) {
                    Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
                    e.printStackTrace();
                }
                if (bindResult && this.mITrackBinder != null) {
                    if (this.D) {
                        Log.d(TAG, "bind success and json data call trackEvent");
                    }
                    track(jsonData);
                } else {
                    Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
                }
            }
        }
    }

    public void track(String key, String value, String eventName) throws RemoteException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new RuntimeException(CAN_NOT_RUN_IN_MAINTHREAD);
        }
        synchronized (GnssOneTrackManager.class) {
            if (this.mBindContext == null) {
                Log.e(TAG, CALL_INIT_FIRST);
                return;
            }
            if (this.mITrackBinder != null) {
                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, eventName);
                    jsonData.put(key, value);
                } catch (JSONException e) {
                    Log.e(TAG, "OneTrack Track Exception:track JSONException");
                }
                if (this.D) {
                    Log.d(TAG, "kv call trackEvent");
                }
                this.mITrackBinder.trackEvent(APP_ID, "com.miui.analytics", jsonData.toString(), 3);
                Log.i(TAG, ONETRACK_TRACK_SUCCESS);
                return;
            }
            if (this.D) {
                Log.d(TAG, "kv No TrackBinder, begin to bindService");
            }
            boolean bindResult = this.mBindContext.bindService(this.intent, this.conn, 1);
            try {
                GnssOneTrackManager.class.wait(1000L);
            } catch (InterruptedException e2) {
                Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
                e2.printStackTrace();
            }
            if (bindResult && this.mITrackBinder != null) {
                if (this.D) {
                    Log.d(TAG, "bind success kv call trackEvent");
                }
                track(key, value, eventName);
            } else {
                Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
            }
            return;
        }
    }

    public void track(List<String> dataList) throws RemoteException {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new RuntimeException(CAN_NOT_RUN_IN_MAINTHREAD);
        }
        synchronized (GnssOneTrackManager.class) {
            if (this.mBindContext == null) {
                Log.e(TAG, CALL_INIT_FIRST);
            } else if (this.mITrackBinder != null) {
                if (this.D) {
                    Log.d(TAG, "data list call trackEvent");
                }
                this.mITrackBinder.trackEvents(APP_ID, "com.miui.analytics", dataList, 3);
                Log.i(TAG, ONETRACK_TRACK_SUCCESS);
            } else {
                if (this.D) {
                    Log.d(TAG, "data list No TrackBinder, begin to bindService");
                }
                boolean bindResult = this.mBindContext.bindService(this.intent, this.conn, 1);
                try {
                    GnssOneTrackManager.class.wait(1000L);
                } catch (InterruptedException e) {
                    Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
                    e.printStackTrace();
                }
                if (bindResult && this.mITrackBinder != null) {
                    if (this.D) {
                        Log.d(TAG, "bind success data list call trackEvent");
                    }
                    track(dataList);
                } else {
                    Log.e(TAG, ONETRACK_TRACK_EXCEPTION);
                }
            }
        }
    }

    public IBinder getGnssOneTrackBinder() {
        Log.d(TAG, "getOneTrackService() on called");
        synchronized (GnssOneTrackManager.class) {
            if (this.mITrackBinder != null) {
                Log.d(TAG, "Return the IBinder");
                return this.mITrackBinder.asBinder();
            }
            Log.d(TAG, "getOneTrackService() failed: no binder!");
            return null;
        }
    }
}
