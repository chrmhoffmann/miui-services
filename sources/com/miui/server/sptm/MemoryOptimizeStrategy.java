package com.miui.server.sptm;

import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.MiuiMemoryServiceInternal;
import com.miui.server.sptm.PreLoadStrategy;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.lang.reflect.Method;
import miui.util.ReflectionUtils;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class MemoryOptimizeStrategy implements SpeedTestModeServiceImpl.Strategy {
    private int[] SPT_MODE_OOM_MIN_FREE;
    private Method mMethodSetEnabledNativeSPTMode;
    private Method mMethodUpdateOomForSPTM;
    private Method mMethodUpdateOomMinFree;
    private MiuiMemoryServiceInternal mMiuiMemoryService;
    private static final String SPTM_OOM_MIN_FREE = SystemProperties.get("persist.sys.miui_sptm.min_free", "73728,92160,110592,240000,340000,450000");
    private static final int MEMORY_RECLAIM_APP_COUNT = SystemProperties.getInt("persist.sys.miui_sptm.start_mem_reclaim_app_count", 12);
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private int mAppCounts = 0;
    private boolean mIsProcCompactEnable = false;
    private ActivityManagerService mAms = ServiceManager.getService("activity");

    public MemoryOptimizeStrategy() {
        this.SPT_MODE_OOM_MIN_FREE = null;
        this.SPT_MODE_OOM_MIN_FREE = loadProperty(SPTM_OOM_MIN_FREE);
        ActivityManagerService activityManagerService = this.mAms;
        if (activityManagerService != null) {
            this.mMethodUpdateOomMinFree = ReflectionUtils.tryFindMethodExact(activityManagerService.getClass(), "updateOomMinFree", new Class[]{int[].class});
            this.mMethodUpdateOomForSPTM = ReflectionUtils.tryFindMethodExact(this.mAms.getClass(), "updateOomForSPTM", new Class[]{Boolean.TYPE});
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(PreLoadStrategy.AppStartRecord r) {
        if (SpeedTestModeServiceImpl.SPEED_TEST_APP_LIST.contains(r.packageName)) {
            if (this.mMiuiMemoryService == null) {
                this.mMiuiMemoryService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
            }
            int i = this.mAppCounts + 1;
            this.mAppCounts = i;
            int i2 = MEMORY_RECLAIM_APP_COUNT;
            if (i >= i2 && !this.mIsProcCompactEnable && this.mSpeedTestModeService.isSpeedTestMode()) {
                MiuiMemoryServiceInternal miuiMemoryServiceInternal = this.mMiuiMemoryService;
                if (miuiMemoryServiceInternal != null) {
                    miuiMemoryServiceInternal.runProcsCompaction(1);
                }
                if (SpeedTestModeServiceImpl.DEBUG) {
                    Slog.d(SpeedTestModeServiceImpl.TAG, String.format("Next Memory Reclaim Apps threshold: %s", Integer.valueOf(i2)));
                }
                this.mIsProcCompactEnable = true;
            }
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
        if (this.SPT_MODE_OOM_MIN_FREE == null || this.mAms == null) {
            return;
        }
        if (!isEnable) {
            this.mAppCounts = 0;
            this.mIsProcCompactEnable = false;
        }
        if (!SpeedTestModeServiceImpl.isLowMemDeviceForSpeedTestMode()) {
            return;
        }
        Method method = this.mMethodSetEnabledNativeSPTMode;
        if (method != null) {
            try {
                method.invoke(this.mAms, Boolean.valueOf(isEnable));
            } catch (Exception e) {
            }
        }
        Method method2 = this.mMethodUpdateOomForSPTM;
        if (method2 != null) {
            try {
                method2.invoke(this.mAms, Boolean.valueOf(isEnable));
            } catch (Exception e2) {
            }
        }
        Method method3 = this.mMethodUpdateOomMinFree;
        if (method3 != null) {
            try {
                ActivityManagerService activityManagerService = this.mAms;
                Object[] objArr = new Object[1];
                objArr[0] = isEnable ? this.SPT_MODE_OOM_MIN_FREE : null;
                method3.invoke(activityManagerService, objArr);
            } catch (Exception e3) {
            }
        }
    }

    private static int[] loadProperty(String oomMinfree) {
        if (TextUtils.isEmpty(oomMinfree)) {
            return null;
        }
        String[] slice = oomMinfree.split(",");
        if (slice.length != 6) {
            return null;
        }
        try {
            int[] res = new int[6];
            for (int i = 0; i < slice.length; i++) {
                res[i] = Integer.parseInt(slice[i]);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
