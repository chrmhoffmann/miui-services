package com.miui.server.migard;

import com.miui.server.migard.utils.LogUtils;
import java.util.ArrayList;
import java.util.List;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class PackageStatusManager {
    private static final String TAG = PackageStatusManager.class.getSimpleName();
    private static PackageStatusManager sInstance = new PackageStatusManager();
    private List<IForegroundChangedCallback> mCallbacks = new ArrayList();
    private IForegroundInfoListener.Stub mForegroundInfoChangeListener = new IForegroundInfoListener.Stub() { // from class: com.miui.server.migard.PackageStatusManager.1
        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) {
            LogUtils.d(PackageStatusManager.TAG, "on foreground info changed, info:" + foregroundInfo);
            for (IForegroundChangedCallback cb : PackageStatusManager.this.mCallbacks) {
                cb.onForegroundChanged(foregroundInfo.mForegroundPid, foregroundInfo.mForegroundUid, foregroundInfo.mForegroundPackageName);
            }
        }
    };

    /* loaded from: classes.dex */
    public interface IForegroundChangedCallback {
        String getCallbackName();

        void onForegroundChanged(int i, int i2, String str);
    }

    public static PackageStatusManager getInstance() {
        return sInstance;
    }

    private PackageStatusManager() {
    }

    public void registerCallback(IForegroundChangedCallback cb) {
        LogUtils.d(TAG, "Register callback, name:" + cb.getCallbackName());
        if (this.mCallbacks.size() == 0) {
            ProcessManager.registerForegroundInfoListener(this.mForegroundInfoChangeListener);
        }
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    public void unregisterCallback(IForegroundChangedCallback cb) {
        LogUtils.d(TAG, "Unregister callback, name:" + cb.getCallbackName());
        if (this.mCallbacks.contains(cb)) {
            this.mCallbacks.remove(cb);
        }
        if (this.mCallbacks.size() == 0) {
            ProcessManager.unregisterForegroundInfoListener(this.mForegroundInfoChangeListener);
        }
    }
}
