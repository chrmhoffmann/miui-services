package miui.android.animation.internal;

import android.util.ArrayMap;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.lang.ref.WeakReference;
import java.util.Map;
import miui.android.animation.IAnimTarget;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.VelocityMonitor;
/* loaded from: classes.dex */
public class TargetVelocityTracker {
    private Map<FloatProperty, MonitorInfo> mMonitors = new ArrayMap();

    /* loaded from: classes.dex */
    public static class MonitorInfo {
        VelocityMonitor monitor;
        ResetRunnable resetTask;

        private MonitorInfo() {
            this.monitor = new VelocityMonitor();
            this.resetTask = new ResetRunnable(this);
        }
    }

    /* loaded from: classes.dex */
    public static class ResetRunnable implements Runnable {
        MonitorInfo mMonitorInfo;
        FloatProperty mProperty;
        WeakReference<IAnimTarget> mTargetRef;

        ResetRunnable(MonitorInfo monitorInfo) {
            this.mMonitorInfo = monitorInfo;
        }

        void post(IAnimTarget target, FloatProperty property) {
            target.handler.removeCallbacks(this);
            WeakReference<IAnimTarget> weakReference = this.mTargetRef;
            if (weakReference == null || weakReference.get() != target) {
                this.mTargetRef = new WeakReference<>(target);
            }
            this.mProperty = property;
            target.handler.postDelayed(this, 600L);
        }

        @Override // java.lang.Runnable
        public void run() {
            IAnimTarget target = this.mTargetRef.get();
            if (target != null) {
                if (!target.isAnimRunning(this.mProperty)) {
                    target.setVelocity(this.mProperty, 0.0d);
                }
                this.mMonitorInfo.monitor.clear();
            }
        }
    }

    public void trackVelocity(IAnimTarget target, FloatProperty property, double value) {
        MonitorInfo info = getMonitor(property);
        info.monitor.update(value);
        float velocity = info.monitor.getVelocity(0);
        if (velocity != MiuiFreeformPinManagerService.EDGE_AREA) {
            info.resetTask.post(target, property);
            target.setVelocity(property, velocity);
        }
    }

    private MonitorInfo getMonitor(FloatProperty property) {
        MonitorInfo info = this.mMonitors.get(property);
        if (info == null) {
            MonitorInfo info2 = new MonitorInfo();
            this.mMonitors.put(property, info2);
            return info2;
        }
        return info;
    }
}
