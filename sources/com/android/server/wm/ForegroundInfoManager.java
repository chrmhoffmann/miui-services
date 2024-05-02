package com.android.server.wm;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.am.ProcessManagerService;
import com.android.server.wm.ActivityRecord;
import java.io.PrintWriter;
import java.util.List;
import miui.process.ForegroundInfo;
import miui.process.IActivityChangeListener;
import miui.process.IForegroundInfoListener;
import miui.process.IForegroundWindowListener;
/* loaded from: classes.dex */
public class ForegroundInfoManager {
    private static final String TAG = "ProcessManager";
    private ComponentName mLastActivityComponent;
    private ProcessManagerService mProcessManagerService;
    private final RemoteCallbackList<IForegroundInfoListener> mForegroundInfoListeners = new RemoteCallbackList<>();
    private final RemoteCallbackList<IActivityChangeListener> mActivityChangeListeners = new RemoteCallbackList<>();
    private final RemoteCallbackList<IForegroundWindowListener> mForegroundWindowListeners = new RemoteCallbackList<>();
    private final Object mForegroundLock = new Object();
    private final Object mActivityLock = new Object();
    private ForegroundInfo mForegroundInfo = new ForegroundInfo();
    private ForegroundInfo mForegroundWindowInfo = new ForegroundInfo();

    /* loaded from: classes.dex */
    public static class FgActivityChangedInfo {
        final ApplicationInfo multiWindowAppInfo;
        final int pid;
        final ActivityRecord record;
        final ActivityRecord.State state;

        public FgActivityChangedInfo(ActivityRecord _record, ActivityRecord.State state, int pid, ApplicationInfo _info) {
            this.record = _record;
            this.state = state;
            this.pid = pid;
            this.multiWindowAppInfo = _info;
        }

        public String getPackageName() {
            return this.record.packageName;
        }
    }

    /* loaded from: classes.dex */
    public static class FgWindowChangedInfo {
        final ApplicationInfo multiWindowAppInfo;
        final int pid;
        final ActivityRecord record;

        public FgWindowChangedInfo(ActivityRecord _record, ApplicationInfo _info, int _pid) {
            this.record = _record;
            this.multiWindowAppInfo = _info;
            this.pid = _pid;
        }
    }

    /* loaded from: classes.dex */
    public class ActivityChangeInfo {
        int callingPid;
        List<String> targetActivities;
        List<String> targetPackages;

        public ActivityChangeInfo(int callingPid, List<String> targetPackages, List<String> targetActivities) {
            ForegroundInfoManager.this = this$0;
            this.callingPid = callingPid;
            this.targetPackages = targetPackages;
            this.targetActivities = targetActivities;
        }

        public String toString() {
            return "ActivityChangeInfo{callingPid=" + this.callingPid + ", targetPackages=" + this.targetPackages + ", targetActivities=" + this.targetActivities + '}';
        }
    }

    public ForegroundInfoManager(ProcessManagerService pms) {
        this.mProcessManagerService = pms;
    }

    private boolean isMultiWindowChanged(ApplicationInfo multiWindowAppInfo) {
        String lastPkg = this.mForegroundInfo.mMultiWindowForegroundPackageName;
        if (multiWindowAppInfo == null) {
            return lastPkg != null;
        }
        return true ^ TextUtils.equals(multiWindowAppInfo.packageName, lastPkg);
    }

    public void notifyForegroundInfoChanged(FgActivityChangedInfo changedInfo) {
        ActivityRecord foregroundRecord = changedInfo.record;
        ActivityRecord.State state = changedInfo.state;
        int pid = changedInfo.pid;
        ApplicationInfo multiWindowAppInfo = changedInfo.multiWindowAppInfo;
        synchronized (this.mForegroundLock) {
            ApplicationInfo foregroundAppInfo = foregroundRecord.info.applicationInfo;
            if (foregroundAppInfo != null && (!TextUtils.equals(this.mForegroundInfo.mForegroundPackageName, foregroundAppInfo.packageName) || isMultiWindowChanged(multiWindowAppInfo) || this.mForegroundInfo.mForegroundUid != foregroundAppInfo.uid)) {
                this.mForegroundInfo.resetFlags();
                if (foregroundRecord.isColdStart && state == ActivityRecord.State.INITIALIZING) {
                    this.mForegroundInfo.addFlags(1);
                }
                ForegroundInfo foregroundInfo = this.mForegroundInfo;
                foregroundInfo.mLastForegroundPackageName = foregroundInfo.mForegroundPackageName;
                ForegroundInfo foregroundInfo2 = this.mForegroundInfo;
                foregroundInfo2.mLastForegroundUid = foregroundInfo2.mForegroundUid;
                ForegroundInfo foregroundInfo3 = this.mForegroundInfo;
                foregroundInfo3.mLastForegroundPid = foregroundInfo3.mForegroundPid;
                this.mForegroundInfo.mForegroundPackageName = foregroundAppInfo.packageName;
                this.mForegroundInfo.mForegroundUid = foregroundAppInfo.uid;
                this.mForegroundInfo.mForegroundPid = pid;
                if (multiWindowAppInfo != null) {
                    this.mForegroundInfo.mMultiWindowForegroundPackageName = multiWindowAppInfo.packageName;
                    this.mForegroundInfo.mMultiWindowForegroundUid = multiWindowAppInfo.uid;
                } else {
                    this.mForegroundInfo.mMultiWindowForegroundPackageName = null;
                    this.mForegroundInfo.mMultiWindowForegroundUid = -1;
                }
                notifyForegroundInfoLocked();
                this.mProcessManagerService.foregroundInfoChanged(this.mForegroundInfo.mForegroundPackageName, foregroundRecord.mActivityComponent);
                ActivityStarterImpl.getInstance().updateLastStartActivityUid(this.mForegroundInfo.mForegroundPackageName, this.mForegroundInfo.mForegroundUid);
                return;
            }
            Log.d("ProcessManager", "skip notify foregroundAppInfo:" + foregroundAppInfo);
        }
    }

    public void notifyForegroundWindowChanged(FgWindowChangedInfo changedInfo) {
        ActivityRecord foregroundRecord = changedInfo.record;
        ActivityRecord.State state = foregroundRecord.getState();
        int pid = changedInfo.pid;
        ApplicationInfo multiWindowAppInfo = changedInfo.multiWindowAppInfo;
        synchronized (this.mForegroundLock) {
            ApplicationInfo foregroundAppInfo = foregroundRecord.info.applicationInfo;
            if (foregroundAppInfo == null) {
                Log.d("ProcessManager", "skip notify foregroundAppInfo:" + foregroundAppInfo);
                return;
            }
            this.mForegroundWindowInfo.resetFlags();
            if (foregroundRecord.isColdStart && state == ActivityRecord.State.INITIALIZING) {
                this.mForegroundWindowInfo.addFlags(1);
            }
            ForegroundInfo foregroundInfo = this.mForegroundWindowInfo;
            foregroundInfo.mLastForegroundPackageName = foregroundInfo.mForegroundPackageName;
            ForegroundInfo foregroundInfo2 = this.mForegroundWindowInfo;
            foregroundInfo2.mLastForegroundUid = foregroundInfo2.mForegroundUid;
            ForegroundInfo foregroundInfo3 = this.mForegroundWindowInfo;
            foregroundInfo3.mLastForegroundPid = foregroundInfo3.mForegroundPid;
            this.mForegroundWindowInfo.mForegroundPackageName = foregroundAppInfo.packageName;
            this.mForegroundWindowInfo.mForegroundUid = foregroundAppInfo.uid;
            this.mForegroundWindowInfo.mForegroundPid = pid;
            if (multiWindowAppInfo != null) {
                this.mForegroundWindowInfo.mMultiWindowForegroundPackageName = multiWindowAppInfo.packageName;
                this.mForegroundWindowInfo.mMultiWindowForegroundUid = multiWindowAppInfo.uid;
            } else {
                this.mForegroundWindowInfo.mMultiWindowForegroundPackageName = null;
                this.mForegroundWindowInfo.mMultiWindowForegroundUid = -1;
            }
            notifyForegroundWindowLocked();
        }
    }

    private void notifyForegroundInfoLocked() {
        for (int i = this.mForegroundInfoListeners.beginBroadcast() - 1; i >= 0; i--) {
            try {
                this.mForegroundInfoListeners.getBroadcastItem(i).onForegroundInfoChanged(this.mForegroundInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mForegroundInfoListeners.finishBroadcast();
    }

    private void notifyForegroundWindowLocked() {
        for (int i = this.mForegroundWindowListeners.beginBroadcast() - 1; i >= 0; i--) {
            try {
                this.mForegroundWindowListeners.getBroadcastItem(i).onForegroundWindowChanged(this.mForegroundWindowInfo);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mForegroundWindowListeners.finishBroadcast();
    }

    public ForegroundInfo getForegroundInfo() {
        ForegroundInfo foregroundInfo;
        synchronized (this.mForegroundLock) {
            foregroundInfo = new ForegroundInfo(this.mForegroundInfo);
        }
        return foregroundInfo;
    }

    public boolean isForegroundApp(String pkgName, int uid) {
        if (uid == -1 || this.mForegroundInfo.mForegroundUid == uid) {
            if (pkgName != null) {
                return pkgName.equals(this.mForegroundInfo.mForegroundPackageName);
            }
            return true;
        }
        return false;
    }

    public void registerForegroundInfoListener(IForegroundInfoListener listener) {
        if (listener != null) {
            synchronized (this.mForegroundLock) {
                this.mForegroundInfoListeners.register(listener);
            }
        }
    }

    public void unregisterForegroundInfoListener(IForegroundInfoListener listener) {
        if (listener != null) {
            synchronized (this.mForegroundLock) {
                this.mForegroundInfoListeners.unregister(listener);
            }
        }
    }

    public void registerForegroundWindowListener(IForegroundWindowListener listener) {
        if (listener != null) {
            synchronized (this.mForegroundLock) {
                this.mForegroundWindowListeners.register(listener);
            }
        }
    }

    public void unregisterForegroundWindowListener(IForegroundWindowListener listener) {
        if (listener != null) {
            synchronized (this.mForegroundLock) {
                this.mForegroundWindowListeners.unregister(listener);
            }
        }
    }

    public void registerActivityChangeListener(List<String> targetPackages, List<String> targetActivities, IActivityChangeListener listener) {
        if (listener != null) {
            synchronized (this.mActivityLock) {
                ActivityChangeInfo info = new ActivityChangeInfo(Binder.getCallingPid(), targetPackages, targetActivities);
                this.mActivityChangeListeners.register(listener, info);
            }
        }
    }

    public void unregisterActivityChangeListener(IActivityChangeListener listener) {
        if (listener != null) {
            synchronized (this.mActivityLock) {
                this.mActivityChangeListeners.unregister(listener);
            }
        }
    }

    public void notifyActivityChanged(ComponentName curActivityComponent) {
        synchronized (this.mActivityLock) {
            if (curActivityComponent != null) {
                if (!curActivityComponent.equals(this.mLastActivityComponent)) {
                    notifyActivitiesChangedLocked(curActivityComponent);
                    this.mLastActivityComponent = curActivityComponent;
                }
            }
        }
    }

    public void notifyActivitiesChangedLocked(ComponentName curComponent) {
        for (int i = this.mActivityChangeListeners.beginBroadcast() - 1; i >= 0; i--) {
            try {
                IActivityChangeListener listener = this.mActivityChangeListeners.getBroadcastItem(i);
                Object cookie = this.mActivityChangeListeners.getBroadcastCookie(i);
                notifyActivityChangedIfNeededLocked(listener, cookie, curComponent);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        this.mActivityChangeListeners.finishBroadcast();
    }

    public void notifyActivityChangedIfNeededLocked(IActivityChangeListener listener, Object cookie, ComponentName curComponent) throws RemoteException {
        if (cookie == null || !(cookie instanceof ActivityChangeInfo)) {
            return;
        }
        ActivityChangeInfo info = (ActivityChangeInfo) cookie;
        ComponentName componentName = this.mLastActivityComponent;
        String lastActivity = null;
        String lastPackage = componentName != null ? componentName.getPackageName() : null;
        ComponentName componentName2 = this.mLastActivityComponent;
        if (componentName2 != null) {
            lastActivity = componentName2.getClassName();
        }
        if (info.targetPackages != null && info.targetActivities != null) {
            if (info.targetPackages.contains(curComponent.getPackageName()) || info.targetPackages.contains(lastPackage) || info.targetActivities.contains(curComponent.getClassName()) || info.targetActivities.contains(lastActivity)) {
                listener.onActivityChanged(this.mLastActivityComponent, curComponent);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println("ForegroundInfo Listener:");
        synchronized (this.mForegroundLock) {
            for (int i = this.mForegroundInfoListeners.beginBroadcast() - 1; i >= 0; i--) {
                pw.print("  #");
                pw.print(i);
                pw.print(": ");
                pw.println(this.mForegroundInfoListeners.getBroadcastItem(i).toString());
            }
            this.mForegroundInfoListeners.finishBroadcast();
        }
        pw.println(prefix + "mForegroundInfo=" + this.mForegroundInfo);
        pw.println("ActivityChange Listener:");
        synchronized (this.mActivityLock) {
            for (int i2 = this.mActivityChangeListeners.beginBroadcast() - 1; i2 >= 0; i2--) {
                pw.print("  #");
                pw.print(i2);
                pw.print(": ");
                pw.println(this.mActivityChangeListeners.getBroadcastCookie(i2));
            }
            this.mActivityChangeListeners.finishBroadcast();
        }
        pw.println(prefix + "mLastActivityComponent=" + this.mLastActivityComponent);
    }
}
