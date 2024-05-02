package com.android.server.wm;

import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import java.io.PrintWriter;
import java.util.HashMap;
/* loaded from: classes.dex */
public class MiuiRefreshRatePolicy implements RefreshRatePolicyStub {
    private final PackageRefreshRate mRefreshRatePackages = new PackageRefreshRate();
    private WindowManagerInternal mWindowManagerInternal;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiRefreshRatePolicy> {

        /* compiled from: MiuiRefreshRatePolicy$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiRefreshRatePolicy INSTANCE = new MiuiRefreshRatePolicy();
        }

        public MiuiRefreshRatePolicy provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiRefreshRatePolicy provideNewInstance() {
            return new MiuiRefreshRatePolicy();
        }
    }

    public static MiuiRefreshRatePolicy getInstance() {
        return (MiuiRefreshRatePolicy) MiuiStubUtil.getImpl(RefreshRatePolicyStub.class);
    }

    public void onSystemReady() {
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PackageRefreshRate {
        private final HashMap<String, Float> mPackages = new HashMap<>();

        PackageRefreshRate() {
            MiuiRefreshRatePolicy.this = this$0;
        }

        public void add(String s, float preferredMaxRefreshRate) {
            this.mPackages.put(s, Float.valueOf(preferredMaxRefreshRate));
        }

        public Float get(String s) {
            return this.mPackages.get(s);
        }

        public void remove(String s) {
            this.mPackages.remove(s);
        }
    }

    public float getSceneRefreshRate(WindowState w) {
        if (w.isFocused()) {
            String pkg = w.getOwningPackage();
            Float preferredMaxRefreshRate = this.mRefreshRatePackages.get(pkg);
            return preferredMaxRefreshRate != null ? preferredMaxRefreshRate.floatValue() : MiuiFreeformPinManagerService.EDGE_AREA;
        }
        return -1.0f;
    }

    public void addSceneRefreshRateForPackage(String packageName, float preferredMaxRefreshRate) {
        synchronized (this.mRefreshRatePackages) {
            this.mRefreshRatePackages.add(packageName, preferredMaxRefreshRate);
        }
        this.mWindowManagerInternal.requestTraversalFromDisplayManager();
    }

    public void removeRefreshRateRangeForPackage(String packageName) {
        synchronized (this.mRefreshRatePackages) {
            this.mRefreshRatePackages.remove(packageName);
        }
        this.mWindowManagerInternal.requestTraversalFromDisplayManager();
    }

    public void dump(PrintWriter pw) {
        pw.println("  RefreshRatePlicy");
        pw.println("    mRefreshRatePackages: " + this.mRefreshRatePackages.mPackages.toString());
    }
}
