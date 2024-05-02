package com.miui.server.migard;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.os.RemoteException;
import com.miui.server.migard.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class UidStateManager {
    private static final String TAG = UidStateManager.class.getSimpleName();
    private static UidStateManager sInstance = new UidStateManager();
    private List<IUidStateChangedCallback> mCallbacks = new ArrayList();
    private IUidObserver mUidObserver = new IUidObserver.Stub() { // from class: com.miui.server.migard.UidStateManager.1
        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
            LogUtils.d(UidStateManager.TAG, "uid cached changed, uid=" + uid);
        }

        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {
            LogUtils.d(UidStateManager.TAG, "uid state changed, uid=" + uid);
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            LogUtils.d(UidStateManager.TAG, "uid gone, uid=" + uid);
            for (IUidStateChangedCallback cb : UidStateManager.this.mCallbacks) {
                cb.onUidGone(uid);
            }
        }

        public void onUidActive(int uid) throws RemoteException {
            LogUtils.d(UidStateManager.TAG, "uid active, uid=" + uid);
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
            LogUtils.d(UidStateManager.TAG, "uid idle, uid=" + uid);
        }

        public void onUidProcAdjChanged(int uid) {
            LogUtils.d(UidStateManager.TAG, "uid changed, uid=" + uid);
        }
    };

    /* loaded from: classes.dex */
    public interface IUidStateChangedCallback {
        String getCallbackName();

        void onUidGone(int i);
    }

    public static UidStateManager getInstance() {
        return sInstance;
    }

    public void registerCallback(IUidStateChangedCallback cb) {
        LogUtils.d(TAG, "Unregister callback, name:" + cb.getCallbackName());
        if (this.mCallbacks.size() == 0) {
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                activityManager.registerUidObserver(this.mUidObserver, 3, -1, (String) null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    public void unregisterCallback(IUidStateChangedCallback cb) {
        LogUtils.d(TAG, "Unregister callback, name:" + cb.getCallbackName());
        if (this.mCallbacks.contains(cb)) {
            this.mCallbacks.remove(cb);
        }
        if (this.mCallbacks.size() == 0) {
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                activityManager.unregisterUidObserver(this.mUidObserver);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
