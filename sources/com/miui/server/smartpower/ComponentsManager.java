package com.miui.server.smartpower;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.util.ArraySet;
import android.view.WindowManager;
import com.android.server.am.AppStateManager;
import com.android.server.am.ProcessRecord;
import com.android.server.am.SmartPowerService;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
/* loaded from: classes.dex */
public class ComponentsManager {
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static final String TAG = "SmartPower";
    private AppStateManager mAppStateManager = null;
    private SmartPowerPolicyManager mSmartPowerPolicyManager = null;
    private final ArraySet<Integer> mPendingUids = new ArraySet<>();

    public ComponentsManager(Context context, Looper looper) {
    }

    public void init(AppStateManager appStateManager, SmartPowerPolicyManager smartPowerPolicyManager) {
        this.mAppStateManager = appStateManager;
        this.mSmartPowerPolicyManager = smartPowerPolicyManager;
    }

    public void updateActivitiesVisibilityLocked() {
        synchronized (this.mPendingUids) {
            this.mAppStateManager.updateVisibilityLocked(this.mPendingUids);
            this.mPendingUids.clear();
        }
    }

    public void activityVisibilityChangedLocked(int uid, String processName, ActivityRecord record, boolean visible) {
        synchronized (this.mPendingUids) {
            this.mPendingUids.add(Integer.valueOf(uid));
            this.mAppStateManager.setActivityVisible(uid, processName, record, visible);
            if (visible) {
                updateActivitiesVisibilityLocked();
            }
        }
    }

    public void windowVisibilityChangedLocked(int uid, int pid, WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
        synchronized (this.mPendingUids) {
            this.mPendingUids.add(Integer.valueOf(uid));
            this.mAppStateManager.setWindowVisible(uid, pid, win, attrs, visible);
            updateActivitiesVisibilityLocked();
        }
    }

    public void activityStartBeforeLocked(String name, int fromPid, int toUid, String packageName, boolean isColdStart) {
        this.mAppStateManager.activityStartBeforeLocked(name, fromPid, toUid, packageName, isColdStart);
    }

    public void onForegroundActivityChangedLocked(int fromPid, int toUid, String name) {
        this.mAppStateManager.onForegroundActivityChangedLocked(fromPid, toUid, name);
    }

    public void broadcastStatusChangedLocked(ProcessRecord processRecord, boolean active, Intent intent) {
        if (active) {
            String content = AppStateManager.REASON_BROADCAST_START + intent.toString();
            this.mAppStateManager.componentStartLocked(processRecord, content);
            return;
        }
        String content2 = AppStateManager.REASON_BROADCAST_END + intent.toString();
        this.mAppStateManager.componentEndLocked(processRecord, content2);
    }

    public void serviceStatusChangedLocked(ProcessRecord processRecord, boolean active, String name, int execution) {
        String content = AppStateManager.REASON_SERVICE_CEANGE + serviceExecutionToString(execution) + " " + name;
        if (active) {
            int state = this.mAppStateManager.getProcessState(processRecord.uid, processRecord.processName);
            if (state > 6) {
                this.mAppStateManager.componentStartLocked(processRecord, content);
                return;
            }
            return;
        }
        this.mAppStateManager.componentEndLocked(processRecord, content);
    }

    public void contentProviderStatusChangedLocked(ProcessRecord hostingProc, String name) {
        if (hostingProc == null) {
            return;
        }
        String content = AppStateManager.REASON_CONTENT_PROVIDER_START + name;
        this.mAppStateManager.componentStartLocked(hostingProc, content);
    }

    public void serviceConnectionChangedLocked(ProcessRecord client, ProcessRecord service, boolean isConnect, int flags) {
        this.mAppStateManager.serviceConnectionChangedLocked(client, service, isConnect, flags);
    }

    public void providerConnectionChangedLocked(ProcessRecord client, ProcessRecord host, boolean isConnect) {
        this.mAppStateManager.providerConnectionChangedLocked(client, host, isConnect);
    }

    public void alarmStatusChangedLocked(int uid, boolean active) {
        if (active) {
            this.mAppStateManager.componentStartLocked(uid, AppStateManager.REASON_ALARM_START);
        } else {
            this.mAppStateManager.componentEndLocked(uid, AppStateManager.REASON_ALARM_END);
        }
    }

    public void onApplyOomAdjLocked(ProcessRecord app) {
        this.mAppStateManager.onApplyOomAdjLocked(app);
    }

    public void onSendPendingIntent(int uid, String typeName) {
        this.mAppStateManager.onSendPendingIntent(uid, typeName);
    }

    static String serviceExecutionToString(int execution) {
        switch (execution) {
            case 0:
                return "end";
            case 1:
                return "bind";
            case 2:
                return "create";
            case 3:
                return "start";
            case 4:
                return "destroy";
            case 5:
                return "unbind";
            default:
                return Integer.toString(execution);
        }
    }
}
