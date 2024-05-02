package com.miui.server.smartpower;

import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public abstract class AppPowerResource {
    protected static final boolean DEBUG = AppPowerResourceManager.DEBUG;
    protected static final String TAG = "SmartPower.AppResource";
    public int mType;
    final SparseArray<List<IAppPowerResourceCallback>> mResourceCallbacksByUid = new SparseArray<>();
    final SparseArray<List<IAppPowerResourceCallback>> mResourceCallbacksByPid = new SparseArray<>();

    /* loaded from: classes.dex */
    public interface IAppPowerResourceCallback {
        void noteResourceActive(int i, int i2);

        void noteResourceInactive(int i, int i2);
    }

    public abstract ArrayList getActiveUids();

    public abstract boolean isAppResourceActive(int i);

    public abstract boolean isAppResourceActive(int i, int i2);

    public abstract void releaseAppPowerResource(int i);

    public abstract void resumeAppPowerResource(int i);

    public void registerCallback(IAppPowerResourceCallback callback, int uid) {
        synchronized (this.mResourceCallbacksByUid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByUid.get(uid);
            if (callbacks == null) {
                callbacks = new ArrayList();
                this.mResourceCallbacksByUid.put(uid, callbacks);
            }
            if (!callbacks.contains(callback)) {
                callbacks.add(callback);
            }
        }
    }

    public void unRegisterCallback(IAppPowerResourceCallback callback, int uid) {
        synchronized (this.mResourceCallbacksByUid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByUid.get(uid);
            if (callbacks != null && callbacks.contains(callback)) {
                callbacks.remove(callback);
                if (callbacks.size() == 0) {
                    this.mResourceCallbacksByUid.remove(uid);
                }
            }
        }
    }

    public void reportResourceStatus(int uid, boolean active, int behavier) {
        synchronized (this.mResourceCallbacksByUid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByUid.get(uid);
            if (callbacks != null) {
                for (IAppPowerResourceCallback callback : callbacks) {
                    if (active) {
                        callback.noteResourceActive(this.mType, behavier);
                    } else {
                        callback.noteResourceInactive(this.mType, behavier);
                    }
                }
            }
        }
    }

    public void registerCallback(IAppPowerResourceCallback callback, int uid, int pid) {
        synchronized (this.mResourceCallbacksByPid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByPid.get(pid);
            if (callbacks == null) {
                callbacks = new ArrayList();
                this.mResourceCallbacksByPid.put(pid, callbacks);
            }
            if (!callbacks.contains(callback)) {
                callbacks.add(callback);
            }
        }
    }

    public void unRegisterCallback(IAppPowerResourceCallback callback, int uid, int pid) {
        synchronized (this.mResourceCallbacksByPid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByPid.get(pid);
            if (callbacks != null && callbacks.contains(callback)) {
                callbacks.remove(callback);
                if (callbacks.size() == 0) {
                    this.mResourceCallbacksByPid.remove(pid);
                }
            }
        }
    }

    public void reportResourceStatus(int uid, int pid, boolean active, int behavier) {
        synchronized (this.mResourceCallbacksByPid) {
            List<IAppPowerResourceCallback> callbacks = this.mResourceCallbacksByPid.get(pid);
            if (callbacks != null) {
                for (IAppPowerResourceCallback callback : callbacks) {
                    if (active) {
                        callback.noteResourceActive(this.mType, behavier);
                    } else {
                        callback.noteResourceInactive(this.mType, behavier);
                    }
                }
            }
        }
    }
}
