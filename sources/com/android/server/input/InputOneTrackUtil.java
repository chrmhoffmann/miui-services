package com.android.server.input;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.miui.analytics.ITrackBinder;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class InputOneTrackUtil {
    private static final String APP_ID_EDGE_SUPPRESSION = "31000000735";
    private static final String APP_ID_KEYBOARD_STYLUS = "31000000824";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final String KEYBOARD_FN_FUNCTION_STATUS_DATA_NAME = "hotkeys_type";
    private static final String KEYBOARD_FN_FUNCTION_STATUS_EVENT_VALUE = "hotkeys_use";
    private static final String KEYBOARD_FN_FUNCTION_STATUS_TIP_NAME = "tip";
    private static final String KEYBOARD_FN_FUNCTION_TRIGGER_TIP = "899.4.0.1.20553";
    private static final String PACKAGE_NAME_EDGE_SUPPRESSION = "com.xiaomi.edgesuppression";
    private static final String PACKAGE_NAME_KEYBOARD_STYLUS = "com.xiaomi.extendDevice";
    private static final String SERVICE_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    private static final String STATUS_EVENT_NAME = "EVENT_NAME";
    private static final String SUPPRESSION_FUNCTION_INITIATIVE = "initiative_settings_by_user";
    private static final String SUPPRESSION_FUNCTION_SIZE = "edge_size";
    private static final String SUPPRESSION_FUNCTION_SIZE_CHANGED = "edge_size_changed";
    private static final String SUPPRESSION_FUNCTION_TYPE = "edge_type";
    private static final String SUPPRESSION_FUNCTION_TYPE_CHANGED = "edge_type_changed";
    private static final String SUPPRESSION_TRACK_TYPE = "edge_suppression";
    private static final String TAG = "InputOneTrackUtil";
    private static volatile InputOneTrackUtil sInstance;
    private final ServiceConnection mConnection = new ServiceConnection() { // from class: com.android.server.input.InputOneTrackUtil.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            InputOneTrackUtil.this.mService = ITrackBinder.Stub.asInterface(service);
            Slog.d(InputOneTrackUtil.TAG, "onServiceConnected: " + InputOneTrackUtil.this.mService);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            InputOneTrackUtil.this.mService = null;
            Slog.d(InputOneTrackUtil.TAG, "onServiceDisconnected");
        }
    };
    private Context mContext;
    private boolean mIsBound;
    private ITrackBinder mService;

    public static InputOneTrackUtil getInstance(Context context) {
        if (sInstance == null) {
            synchronized (InputOneTrackUtil.class) {
                if (sInstance == null) {
                    sInstance = new InputOneTrackUtil(context);
                }
            }
        }
        return sInstance;
    }

    private InputOneTrackUtil(Context context) {
        bindTrackService(context);
    }

    private void bindTrackService(Context context) {
        this.mContext = context;
        Intent intent = new Intent();
        intent.setClassName("com.miui.analytics", SERVICE_NAME);
        this.mIsBound = this.mContext.bindServiceAsUser(intent, this.mConnection, 1, UserHandle.SYSTEM);
        Slog.d(TAG, "bindTrackService: " + this.mIsBound);
    }

    public void unbindTrackService(Context context) {
        if (this.mIsBound) {
            context.unbindService(this.mConnection);
        }
    }

    public void trackEvent(String appId, String packageName, String data, int flag) {
        if (this.mService == null) {
            Slog.d(TAG, "trackEvent: track service not bound");
            bindTrackService(this.mContext);
        }
        try {
            ITrackBinder iTrackBinder = this.mService;
            if (iTrackBinder != null) {
                iTrackBinder.trackEvent(appId, packageName, data, flag);
            } else {
                Slog.e(TAG, "trackEvent: track service is null");
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "trackEvent: " + e.getMessage());
        }
    }

    public void trackKeyboardEvent(String data) {
        trackEvent(APP_ID_KEYBOARD_STYLUS, PACKAGE_NAME_KEYBOARD_STYLUS, getKeyboardStatusData(data), 2);
    }

    public void trackEdgeSuppressionEvent(boolean changed, int changedSize, String changedType, int size, String type) {
        trackEvent(APP_ID_EDGE_SUPPRESSION, PACKAGE_NAME_EDGE_SUPPRESSION, getEdgeSuppressionStatusData(changed, changedSize, changedType, size, type), 2);
    }

    private String getKeyboardStatusData(String function) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", KEYBOARD_FN_FUNCTION_STATUS_EVENT_VALUE);
            jsonObject.put(KEYBOARD_FN_FUNCTION_STATUS_DATA_NAME, function);
            jsonObject.put(KEYBOARD_FN_FUNCTION_STATUS_TIP_NAME, KEYBOARD_FN_FUNCTION_TRIGGER_TIP);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }

    private String getEdgeSuppressionStatusData(boolean changed, int changedSize, String changedType, int size, String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("EVENT_NAME", SUPPRESSION_TRACK_TYPE);
            jsonObject.put(SUPPRESSION_FUNCTION_INITIATIVE, changed);
            jsonObject.put(SUPPRESSION_FUNCTION_SIZE_CHANGED, changedSize);
            jsonObject.put(SUPPRESSION_FUNCTION_TYPE_CHANGED, changedType);
            jsonObject.put(SUPPRESSION_FUNCTION_SIZE, size);
            jsonObject.put(SUPPRESSION_FUNCTION_TYPE, type);
        } catch (Exception exception) {
            Slog.e(TAG, "construct TrackEvent data fail!" + exception.toString());
        }
        return jsonObject.toString();
    }
}
