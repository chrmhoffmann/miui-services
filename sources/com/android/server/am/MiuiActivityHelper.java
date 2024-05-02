package com.android.server.am;

import android.app.ActivityManagerNative;
import android.os.Process;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public abstract class MiuiActivityHelper {
    private static final String TAG = "MiuiActivityHelper";
    private static long sTotalMem = Process.getTotalMemory();
    static ActivityManagerService sAms = null;

    private static native long getNativeCachedLostMemory();

    private static native long getNativeFreeMemory();

    public MiuiActivityHelper() {
        System.loadLibrary("miui_security");
    }

    private static long getCachePss() {
        ArrayList<ProcessRecord> procs;
        if (sAms == null) {
            ActivityManagerService activityManagerService = ActivityManagerNative.getDefault();
            if (activityManagerService instanceof ActivityManagerService) {
                sAms = activityManagerService;
            }
        }
        long cachePss = 0;
        ActivityManagerService activityManagerService2 = sAms;
        if (activityManagerService2 != null && (procs = activityManagerService2.collectProcesses((PrintWriter) null, 0, false, (String[]) null)) != null) {
            Iterator<ProcessRecord> it = procs.iterator();
            while (it.hasNext()) {
                ProcessRecord proc = it.next();
                synchronized (sAms.mProcLock) {
                    if (proc.mState.getSetAdj() >= 900) {
                        cachePss += proc.mProfile.getLastPss();
                    }
                }
            }
        }
        return 1024 * cachePss;
    }

    public static long getCachedLostRam() {
        return getNativeCachedLostMemory();
    }

    public static long getFreeMemory() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long[] rawInfo = minfo.getRawInfo();
        Slog.d(TAG, "MEMINFO_KRECLAIMABLE: " + rawInfo[15] + ", MEMINFO_SLAB_RECLAIMABLE: " + rawInfo[6] + ", MEMINFO_BUFFERS: " + rawInfo[2] + ", MEMINFO_CACHED: " + rawInfo[3] + ", MEMINFO_FREE: " + rawInfo[1]);
        long kReclaimable = rawInfo[15];
        if (kReclaimable == 0) {
            kReclaimable = rawInfo[6];
        }
        long cache = (rawInfo[2] + kReclaimable + rawInfo[3] + rawInfo[1]) * 1024;
        long lostCache = getCachedLostRam();
        long free = cache + lostCache + getCachePss();
        Slog.d(TAG, "cache: " + cache + ", nativefree: " + getNativeFreeMemory() + "lostcache: " + lostCache);
        if (free >= sTotalMem) {
            return cache + lostCache;
        }
        return free;
    }
}
