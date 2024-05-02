package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.miui.analytics.ITrackBinder;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class PreInstallServiceTrack {
    private static final String APP_ID = "31000000142";
    private static final String PACKAGE_NAME = "com.xiaomi.preload";
    private static final String SERVICE_NAME = "com.miui.analytics.onetrack.TrackService";
    private static final String SERVICE_PACKAGE_NAME = "com.miui.analytics";
    private static String TAG = "PreInstallServiceTrack";
    private final ServiceConnection mConnection = new ServiceConnection() { // from class: com.android.server.pm.PreInstallServiceTrack.1
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName name, IBinder service) {
            PreInstallServiceTrack.this.mService = ITrackBinder.Stub.asInterface(service);
            Slog.d(PreInstallServiceTrack.TAG, "onServiceConnected: " + PreInstallServiceTrack.this.mService);
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName name) {
            PreInstallServiceTrack.this.mService = null;
            Slog.d(PreInstallServiceTrack.TAG, "onServiceDisconnected");
        }
    };
    private boolean mIsBound;
    private ITrackBinder mService;

    public void bindTrackService(Context context) {
        Intent intent = new Intent();
        intent.setClassName("com.miui.analytics", SERVICE_NAME);
        this.mIsBound = context.bindService(intent, this.mConnection, 1);
        Slog.d(TAG, "bindTrackService: " + this.mIsBound);
    }

    public void unbindTrackService(Context context) {
        if (this.mIsBound) {
            context.unbindService(this.mConnection);
        }
    }

    public void trackEvent(String data, int flags) {
        ITrackBinder iTrackBinder = this.mService;
        if (iTrackBinder == null) {
            Slog.d(TAG, "trackEvent: track service not bound");
            return;
        }
        try {
            iTrackBinder.trackEvent(APP_ID, PACKAGE_NAME, data, flags);
        } catch (RemoteException e) {
            Slog.e(TAG, "trackEvent: " + e.getMessage());
        }
    }

    /* loaded from: classes.dex */
    public static class Action {
        protected static final String ACTION_KEY = "_action_";
        protected static final String CATEGORY = "_category_";
        protected static final String EVENT_ID = "_event_id_";
        protected static final String LABEL = "_label_";
        protected static final String VALUE = "_value_";
        private JSONObject mContent = new JSONObject();
        private JSONObject mExtra = new JSONObject();
        private Set<String> sKeywords;

        public Action() {
            HashSet hashSet = new HashSet();
            this.sKeywords = hashSet;
            hashSet.add(EVENT_ID);
            this.sKeywords.add(CATEGORY);
            this.sKeywords.add(ACTION_KEY);
            this.sKeywords.add(LABEL);
            this.sKeywords.add(VALUE);
        }

        public Action addParam(String key, JSONObject value) {
            ensureKey(key);
            addContent(key, value);
            return this;
        }

        public Action addParam(String key, int value) {
            ensureKey(key);
            addContent(key, value);
            return this;
        }

        public Action addParam(String key, long value) {
            ensureKey(key);
            addContent(key, value);
            return this;
        }

        public Action addParam(String key, String value) {
            ensureKey(key);
            addContent(key, value);
            return this;
        }

        public void addContent(String key, int value) {
            if (!TextUtils.isEmpty(key)) {
                try {
                    this.mContent.put(key, value);
                } catch (Exception e) {
                    Log.e(PreInstallServiceTrack.TAG, e.getMessage());
                }
            }
        }

        public void addContent(String key, long value) {
            if (!TextUtils.isEmpty(key)) {
                try {
                    this.mContent.put(key, value);
                } catch (Exception e) {
                    Log.e(PreInstallServiceTrack.TAG, e.getMessage());
                }
            }
        }

        public void addContent(String key, Object value) {
            if (!TextUtils.isEmpty(key)) {
                try {
                    this.mContent.put(key, value);
                } catch (Exception e) {
                    Log.e(PreInstallServiceTrack.TAG, e.getMessage());
                }
            }
        }

        private void ensureKey(String key) {
            if (!TextUtils.isEmpty(key) && this.sKeywords.contains(key)) {
                throw new IllegalArgumentException("this key " + key + " is built-in, please pick another key.");
            }
        }

        public final JSONObject getContent() {
            return this.mContent;
        }

        final JSONObject getExtra() {
            return this.mExtra;
        }
    }
}
