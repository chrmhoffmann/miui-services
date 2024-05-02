package com.android.server.power;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.android.internal.app.IUidStateChangeCallback;
import com.android.internal.os.BackgroundThread;
/* loaded from: classes.dex */
public class UidStateHelper {
    private static final int MSG_DISPATCH_UID_STATE_CHANGE = 1;
    private static UidStateHelper sInstance;
    private final IActivityManager mActivityManager;
    private boolean mObserverInstalled;
    private static String TAG = "UidProcStateHelper";
    private static boolean DEBUG = Build.IS_DEBUGGABLE;
    private Object mStateLock = new Object();
    private final SparseBooleanArray mUidForeground = new SparseBooleanArray();
    private final SparseArray<SparseBooleanArray> mUidPidForeground = new SparseArray<>();
    private RemoteCallbackList<IUidStateChangeCallback> mUidStateObervers = new RemoteCallbackList<>();
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.power.UidStateHelper.1
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            if (UidStateHelper.DEBUG) {
                Slog.v(UidStateHelper.TAG, "foreground changed:[" + pid + "," + uid + "," + foregroundActivities + "]");
            }
            synchronized (UidStateHelper.this.mStateLock) {
                SparseBooleanArray pidForeground = (SparseBooleanArray) UidStateHelper.this.mUidPidForeground.get(uid);
                if (pidForeground == null) {
                    pidForeground = new SparseBooleanArray(2);
                    UidStateHelper.this.mUidPidForeground.put(uid, pidForeground);
                }
                pidForeground.put(pid, foregroundActivities);
                UidStateHelper.this.computeUidForegroundLocked(uid);
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
            if (UidStateHelper.DEBUG) {
                Slog.v(UidStateHelper.TAG, "foreground changed:[" + pid + "," + uid + "," + serviceTypes + "]");
            }
        }

        public void onProcessDied(int pid, int uid) {
            if (UidStateHelper.DEBUG) {
                Slog.v(UidStateHelper.TAG, "process died:[" + pid + "," + uid + "]");
            }
            synchronized (UidStateHelper.this.mStateLock) {
                SparseBooleanArray pidForeground = (SparseBooleanArray) UidStateHelper.this.mUidPidForeground.get(uid);
                if (pidForeground != null) {
                    pidForeground.delete(pid);
                    UidStateHelper.this.computeUidForegroundLocked(uid);
                }
            }
        }
    };
    private Handler.Callback mHandlerCallback = new Handler.Callback() { // from class: com.android.server.power.UidStateHelper.2
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int uid = msg.arg1;
                    int state = msg.arg2;
                    UidStateHelper.this.dispatchUidStateChange(uid, state);
                    return true;
                default:
                    return false;
            }
        }
    };
    private final Handler mHandler = new Handler(BackgroundThread.get().getLooper(), this.mHandlerCallback);

    public static UidStateHelper get() {
        if (sInstance == null) {
            sInstance = new UidStateHelper();
        }
        return sInstance;
    }

    private UidStateHelper() {
        this.mObserverInstalled = false;
        IActivityManager iActivityManager = ActivityManagerNative.getDefault();
        this.mActivityManager = iActivityManager;
        try {
            iActivityManager.registerProcessObserver(this.mProcessObserver);
            this.mObserverInstalled = true;
        } catch (RemoteException e) {
        }
    }

    public void registerUidStateObserver(IUidStateChangeCallback callback) {
        if (!this.mObserverInstalled) {
            throw new IllegalStateException("ProcessObserver not installed");
        }
        synchronized (this) {
            this.mUidStateObervers.register(callback);
        }
    }

    public void unregisterUidStateObserver(IUidStateChangeCallback callback) {
        if (!this.mObserverInstalled) {
            throw new IllegalStateException("ProcessObserver not installed");
        }
        synchronized (this) {
            this.mUidStateObervers.unregister(callback);
        }
    }

    public boolean isUidForeground(int uid) {
        boolean z;
        if (!UserHandle.isApp(uid)) {
            return true;
        }
        synchronized (this.mStateLock) {
            z = this.mUidForeground.get(uid, false);
        }
        return z;
    }

    public boolean isUidForeground(int uid, boolean doubleCheck) {
        boolean z = true;
        if (!UserHandle.isApp(uid)) {
            return true;
        }
        synchronized (this.mStateLock) {
            boolean isUidFg = this.mUidForeground.get(uid, false);
            if (doubleCheck) {
                SparseBooleanArray pidForeground = this.mUidPidForeground.get(uid);
                if (pidForeground != null) {
                    for (int i = 0; i < pidForeground.size(); i++) {
                        pidForeground.keyAt(i);
                    }
                }
                if (isUidFg) {
                    Slog.wtf(TAG, "ProcessObserver may miss callback, isUidFg=" + isUidFg + " isFgByPids=false");
                }
                if (!isUidFg && 0 == 0) {
                    z = false;
                }
                return z;
            }
            return isUidFg;
        }
    }

    public void computeUidForegroundLocked(int uid) {
        SparseBooleanArray pidForeground = this.mUidPidForeground.get(uid);
        boolean uidForeground = false;
        int size = pidForeground.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            } else if (!pidForeground.valueAt(i)) {
                i++;
            } else {
                uidForeground = true;
                break;
            }
        }
        boolean oldUidForeground = this.mUidForeground.get(uid, false);
        if (oldUidForeground != uidForeground) {
            this.mUidForeground.put(uid, uidForeground);
            Handler handler = this.mHandler;
            int i2 = uidForeground ? 1 : 0;
            int i3 = uidForeground ? 1 : 0;
            handler.obtainMessage(1, uid, i2).sendToTarget();
        }
    }

    public void dispatchUidStateChange(int uid, int state) {
        int length = this.mUidStateObervers.beginBroadcast();
        for (int i = 0; i < length; i++) {
            IUidStateChangeCallback callback = this.mUidStateObervers.getBroadcastItem(i);
            if (callback != null) {
                try {
                    callback.onUidStateChange(uid, state);
                } catch (RemoteException e) {
                }
            }
        }
        this.mUidStateObervers.finishBroadcast();
    }
}
