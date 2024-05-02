package com.miui.server.stability;

import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.ScoutHelper;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ScoutMeminfo;
import com.android.server.am.ScoutMemoryError;
import com.android.server.content.SyncManagerStubImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import miui.mqsas.scout.ScoutUtils;
/* loaded from: classes.dex */
public class ScoutDisplayMemoryManager {
    private static final String APP_ID = "31000000454";
    private static final int DISPLAY_DMABUF = 1;
    private static final int DISPLAY_KGSL = 2;
    private static final String DMABUF_EVENT_NAME = "dmabuf_leak";
    private static boolean ENABLE_SCOUT_MEMORY_MONITOR = false;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final long GBTOKB = 1048576;
    private static int ION_LEAK_THRESHOLD = 0;
    private static final String KGSL_EVENT_NAME = "kgsl_leak";
    private static int KGSL_LEAK_THRESHOLD = 0;
    private static final long MBTOKB = 1024;
    private static final int MEM_DISABLE_REPORT_INTERVAL = 600000;
    private static final int MEM_ERROR_DIALOG_TIMEOUT = 300000;
    private static final int MEM_REPORT_INTERVAL = 3600000;
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String PACKAGE = "android";
    private static final double PROC_PROPORTIONAL_THRESHOLD = 0.6d;
    private static final String PROP_LOST_RAM_LEAK_DUMP = "sys.memleak.lost_ram_leak_dump";
    public static final int RESUME_ACTION_CANCLE = 5;
    public static final int RESUME_ACTION_CONFIRM = 4;
    public static final int RESUME_ACTION_CRASH = 2;
    public static final int RESUME_ACTION_DIALOG = 3;
    public static final int RESUME_ACTION_FAIL = 0;
    public static final int RESUME_ACTION_KILL = 1;
    private static boolean SCOUT_MEMORY_DISABLE_KGSL = false;
    private static final String SYSPROP_ENABLE_SCOUT_MEMORY_MONITOR = "persist.sys.debug.enable_scout_memory_monitor";
    private static final String SYSPROP_ENABLE_SCOUT_MEMORY_RESUME = "persist.sys.debug.enable_scout_memory_resume";
    private static final String SYSPROP_ION_LEAK_THRESHOLD = "persist.sys.debug.scout_memory_ion_threshold";
    private static final String SYSPROP_KGSL_LEAK_THRESHOLD = "persist.sys.debug.scout_memory_kgsl_threshold";
    private static final String SYSPROP_SCOUT_MEMORY_DISABLE_KGSL = "persist.sys.debug.scout_memory_disable_kgsl";
    private static final String TAG = "ScoutDisplayMemoryManager";
    private static boolean debug;
    private static ScoutDisplayMemoryManager displayMemoryManager;
    private boolean isCameraForeground;
    private Context mContext;
    private ActivityManagerService mService;
    private volatile long mShowDialogTime;
    private AtomicBoolean isBusy = new AtomicBoolean(false);
    private AtomicBoolean mIsShowDialog = new AtomicBoolean(false);
    private AtomicBoolean mDisableState = new AtomicBoolean(false);
    private int totalRam = 0;
    private volatile long mLastReportTime = 0;

    private native long getTotalKgsl();

    public native DmaBufUsageInfo readDmabufInfo();

    public native KgslUsageInfo readKgslInfo();

    static {
        debug = ScoutHelper.ENABLED_SCOUT_DEBUG || ScoutUtils.isLibraryTest();
        ION_LEAK_THRESHOLD = SystemProperties.getInt(SYSPROP_ION_LEAK_THRESHOLD, 2560);
        KGSL_LEAK_THRESHOLD = SystemProperties.getInt(SYSPROP_KGSL_LEAK_THRESHOLD, 2560);
        ENABLE_SCOUT_MEMORY_MONITOR = SystemProperties.getBoolean(SYSPROP_ENABLE_SCOUT_MEMORY_MONITOR, false);
        SCOUT_MEMORY_DISABLE_KGSL = SystemProperties.getBoolean(SYSPROP_SCOUT_MEMORY_DISABLE_KGSL, false);
    }

    public static ScoutDisplayMemoryManager getInstance() {
        if (displayMemoryManager == null) {
            displayMemoryManager = new ScoutDisplayMemoryManager();
        }
        return displayMemoryManager;
    }

    private ScoutDisplayMemoryManager() {
    }

    public void init(ActivityManagerService mService, Context mContext) {
        this.mService = mService;
        this.mContext = mContext;
        ScoutMemoryError.getInstance().init(mService, mContext);
    }

    public boolean isEnableScoutMemory() {
        return ENABLE_SCOUT_MEMORY_MONITOR;
    }

    public boolean isEnableResumeFeature() {
        return ScoutUtils.isLibraryTest() || ScoutUtils.isUnReleased() || SystemProperties.getBoolean(SYSPROP_ENABLE_SCOUT_MEMORY_RESUME, false);
    }

    public void updateCameraForegroundState(boolean isCameraForeground) {
        this.isCameraForeground = isCameraForeground;
    }

    public void updateLastReportTime() {
        this.mLastReportTime = SystemClock.uptimeMillis();
    }

    public void setDisableState(boolean state) {
        this.mDisableState.set(state);
    }

    public void setShowDialogState(boolean state) {
        this.mIsShowDialog.set(state);
        if (state) {
            this.mShowDialogTime = SystemClock.uptimeMillis();
        }
    }

    private int getTotalRam() {
        int i = this.totalRam;
        if (i > 0) {
            return i;
        }
        long[] infos = new long[19];
        Debug.getMemInfo(infos);
        this.totalRam = (int) (infos[0] / GBTOKB);
        if (debug) {
            Slog.d("MIUIScout Memory", "getTotalRam total memory " + infos[0] + " kB, " + this.totalRam + "GB");
        }
        return this.totalRam;
    }

    public void reportDisplayMemoryLeakEvent(final DiaplayMemoryErrorInfo errorInfo) {
        if (this.mContext == null || errorInfo == null) {
            return;
        }
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ScoutDisplayMemoryManager.this.m2447xb5441398(errorInfo);
            }
        });
    }

    /* renamed from: lambda$reportDisplayMemoryLeakEvent$0$com-miui-server-stability-ScoutDisplayMemoryManager */
    public /* synthetic */ void m2447xb5441398(DiaplayMemoryErrorInfo errorInfo) {
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage("com.miui.analytics");
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, PACKAGE);
        if (errorInfo.getType() == 1) {
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, DMABUF_EVENT_NAME);
            intent.putExtra("dmabuf_total_size", String.valueOf(errorInfo.getTotalSize()));
        } else if (errorInfo.getType() == 2) {
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, KGSL_EVENT_NAME);
            intent.putExtra("kgsl_total_size", String.valueOf(errorInfo.getTotalSize()));
        } else {
            return;
        }
        intent.putExtra("memory_total_size", getTotalRam());
        intent.putExtra("memory_app_package", errorInfo.getName());
        intent.putExtra("memory_app_size", String.valueOf(errorInfo.getRss()));
        intent.putExtra("memory_app_adj", String.valueOf(errorInfo.getOomadj()));
        intent.putExtra("resume_action", String.valueOf(errorInfo.getAction()));
        intent.setFlags(1);
        if (debug) {
            Slog.d("MIUIScout Memory", "report type=" + errorInfo.getType() + " memory_total_size=" + getTotalRam() + " total_size=" + errorInfo.getTotalSize() + " memory_app_package=" + errorInfo.getName() + " memory_app_size =" + errorInfo.getRss() + " memory_app_adj=" + errorInfo.getOomadj() + " action = " + errorInfo.getAction());
        }
        try {
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e(TAG, "Upload onetrack exception!", e);
        }
    }

    static String stringifySize(long size, int order) {
        Locale locale = Locale.US;
        switch (order) {
            case 1:
                return String.format(locale, "%,13d", Long.valueOf(size));
            case 1024:
                return String.format(locale, "%,9dkB", Long.valueOf(size / MBTOKB));
            case 1048576:
                return String.format(locale, "%,5dMB", Long.valueOf((size / MBTOKB) / MBTOKB));
            case 1073741824:
                return String.format(locale, "%,1dGB", Long.valueOf(((size / MBTOKB) / MBTOKB) / MBTOKB));
            default:
                throw new IllegalArgumentException("Invalid size order");
        }
    }

    static String stringifyKBSize(long size) {
        return stringifySize(MBTOKB * size, 1024);
    }

    /* JADX WARN: Removed duplicated region for block: B:36:0x015b  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x01a8  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void resumeMemLeak(com.miui.server.stability.ScoutDisplayMemoryManager.DiaplayMemoryErrorInfo r23) {
        /*
            Method dump skipped, instructions count: 568
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.stability.ScoutDisplayMemoryManager.resumeMemLeak(com.miui.server.stability.ScoutDisplayMemoryManager$DiaplayMemoryErrorInfo):void");
    }

    private void reportDmabufLeakException(final String reason, final long totalSize) {
        if (this.isBusy.compareAndSet(false, true)) {
            new Thread(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.1
                @Override // java.lang.Runnable
                public void run() {
                    Slog.e("MIUIScout Memory", "Warring!!! dma-buf leak:" + ScoutDisplayMemoryManager.stringifyKBSize(totalSize) + (ScoutDisplayMemoryManager.this.isCameraForeground ? " , camera in the foreground" : ""));
                    String dambufLog = "";
                    DmaBufUsageInfo dmabufInfo = ScoutDisplayMemoryManager.this.readDmabufInfo();
                    ArrayList<DmaBufProcUsageInfo> infoList = null;
                    if (dmabufInfo != null) {
                        infoList = dmabufInfo.getList();
                        Collections.sort(infoList, new Comparator<DmaBufProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.1.1
                            public int compare(DmaBufProcUsageInfo o1, DmaBufProcUsageInfo o2) {
                                if (o2.getRss() == o1.getRss()) {
                                    return 0;
                                }
                                if (o2.getRss() > o1.getRss()) {
                                    return 1;
                                }
                                return -1;
                            }
                        });
                        dambufLog = dmabufInfo.toString();
                        Slog.e("MIUIScout Memory", dambufLog);
                        if (dmabufInfo.getTotalSize() < ScoutDisplayMemoryManager.ION_LEAK_THRESHOLD * ScoutDisplayMemoryManager.MBTOKB) {
                            String failMsg = "TotalSize " + ScoutDisplayMemoryManager.stringifyKBSize(totalSize) + " dma-buf, DmabufInfo totalSize " + dmabufInfo.getTotalSize() + "kB less than threshold, Skip";
                            Slog.e("MIUIScout Memory", failMsg);
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                    }
                    DmaBufProcUsageInfo topProc = null;
                    if (infoList != null) {
                        ArrayList<Integer> fdPids = new ArrayList<>(5);
                        Iterator<DmaBufProcUsageInfo> it = infoList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            DmaBufProcUsageInfo procInfo = it.next();
                            String procName = procInfo.getName();
                            if (ScoutMemoryUtils.skipIonProcList.contains(procName)) {
                                fdPids.add(Integer.valueOf(procInfo.getPid()));
                                Slog.e("MIUIScout Memory", "Skip " + procName + "(pid=" + procInfo.getPid() + " adj=" + procInfo.getOomadj() + ")");
                            } else {
                                topProc = procInfo;
                                fdPids.add(Integer.valueOf(topProc.getPid()));
                                Slog.e("MIUIScout Memory", "Most used process name=" + procName + " pid=" + procInfo.getPid() + " adj=" + procInfo.getOomadj() + " rss=" + procInfo.getRss());
                                break;
                            }
                        }
                        if (topProc != null) {
                            fdPids.add(Integer.valueOf(Process.myPid()));
                            ScoutMemoryUtils.captureIonLeakLog(dambufLog, reason, fdPids);
                            long thresholdSize = (long) (ScoutDisplayMemoryManager.ION_LEAK_THRESHOLD * ScoutDisplayMemoryManager.MBTOKB * ScoutDisplayMemoryManager.PROC_PROPORTIONAL_THRESHOLD);
                            DiaplayMemoryErrorInfo errorInfo = new DiaplayMemoryErrorInfo(topProc, thresholdSize, totalSize);
                            if (topProc.getRss() > thresholdSize) {
                                if (ScoutDisplayMemoryManager.this.isEnableResumeFeature()) {
                                    ScoutDisplayMemoryManager.this.resumeMemLeak(errorInfo);
                                } else {
                                    ScoutDisplayMemoryManager.this.reportDisplayMemoryLeakEvent(errorInfo);
                                }
                            }
                            ScoutDisplayMemoryManager.this.updateLastReportTime();
                        }
                    }
                    ScoutDisplayMemoryManager.this.isBusy.set(false);
                }
            }, reason + "-" + stringifyKBSize(totalSize)).start();
        } else {
            Slog.d("MIUIScout Memory", "Is Busy! skip report Dma-buf Leak Exception");
        }
    }

    private void reportKgslLeakException(final String reason, final long totalSize) {
        if (this.isBusy.compareAndSet(false, true)) {
            new Thread(new Runnable() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.2
                @Override // java.lang.Runnable
                public void run() {
                    String kgslLog = "";
                    KgslUsageInfo kgslInfo = ScoutDisplayMemoryManager.this.readKgslInfo();
                    ArrayList<KgslProcUsageInfo> infoList = null;
                    if (kgslInfo != null) {
                        infoList = kgslInfo.getList();
                        Collections.sort(infoList, new Comparator<KgslProcUsageInfo>() { // from class: com.miui.server.stability.ScoutDisplayMemoryManager.2.1
                            public int compare(KgslProcUsageInfo o1, KgslProcUsageInfo o2) {
                                if (o2.getRss() == o1.getRss()) {
                                    return 0;
                                }
                                if (o2.getRss() > o1.getRss()) {
                                    return 1;
                                }
                                return -1;
                            }
                        });
                        kgslLog = kgslInfo.toString();
                        Slog.e("MIUIScout Memory", kgslLog);
                        if (kgslInfo.getTotalSize() < ScoutDisplayMemoryManager.KGSL_LEAK_THRESHOLD * ScoutDisplayMemoryManager.MBTOKB) {
                            String failMsg = "TotalSize " + ScoutDisplayMemoryManager.stringifyKBSize(totalSize) + " kgsl, KgslInfo totalSize " + kgslInfo.getTotalSize() + "kB less than threshold, Skip";
                            Slog.e("MIUIScout Memory", failMsg);
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                    }
                    if (kgslInfo != null && infoList != null && infoList.size() > 0) {
                        KgslProcUsageInfo topProc = infoList.get(0);
                        Slog.e("MIUIScout Memory", "Most used process name=" + topProc.getName() + " pid=" + topProc.getPid() + " adj=" + topProc.getOomadj() + " size=" + topProc.getRss() + "kB");
                        if (ScoutMemoryUtils.kgslWhiteList.contains(topProc.getName())) {
                            Slog.e("MIUIScout Memory", topProc.getName() + " is white app, skip");
                            ScoutDisplayMemoryManager.this.isBusy.set(false);
                            return;
                        }
                        ScoutMemoryUtils.captureKgslLeakLog(kgslLog, reason);
                        long thresholdSize = (long) (ScoutDisplayMemoryManager.KGSL_LEAK_THRESHOLD * ScoutDisplayMemoryManager.MBTOKB * ScoutDisplayMemoryManager.PROC_PROPORTIONAL_THRESHOLD);
                        DiaplayMemoryErrorInfo errorInfo = new DiaplayMemoryErrorInfo(topProc, thresholdSize, totalSize);
                        if (topProc.getRss() > thresholdSize) {
                            if (ScoutDisplayMemoryManager.this.isEnableResumeFeature()) {
                                ScoutDisplayMemoryManager.this.resumeMemLeak(errorInfo);
                            } else {
                                ScoutDisplayMemoryManager.this.reportDisplayMemoryLeakEvent(errorInfo);
                            }
                        }
                        ScoutDisplayMemoryManager.this.updateLastReportTime();
                    }
                    ScoutDisplayMemoryManager.this.isBusy.set(false);
                }
            }, reason + "-" + stringifyKBSize(totalSize)).start();
        } else {
            Slog.d("MIUIScout Memory", "Is Busy! skip report Kgsl Leak Exception");
        }
    }

    private boolean checkDmaBufLeak(ScoutMeminfo scoutMeminfo) {
        long dmaBufTotal;
        long ionHeap = Debug.getIonHeapsSizeKb();
        if (ionHeap >= 0) {
            dmaBufTotal = ionHeap;
        } else {
            dmaBufTotal = scoutMeminfo.getTotalExportedDmabuf();
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "checkDmaBufLeak(root) size:" + stringifyKBSize(dmaBufTotal) + " monitor threshold = " + (ION_LEAK_THRESHOLD * MBTOKB) + "kB");
        }
        if (dmaBufTotal > ION_LEAK_THRESHOLD * MBTOKB) {
            reportDmabufLeakException(DMABUF_EVENT_NAME, dmaBufTotal);
            return true;
        }
        return false;
    }

    private boolean checkDmaBufLeak() {
        long dmaBufTotal;
        long ionHeap = Debug.getIonHeapsSizeKb();
        if (ionHeap >= 0) {
            dmaBufTotal = ionHeap;
        } else {
            dmaBufTotal = Debug.getDmabufTotalExportedKb();
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "checkDmaBufLeak size:" + stringifyKBSize(dmaBufTotal) + " monitor threshold = " + (ION_LEAK_THRESHOLD * MBTOKB) + "kB");
        }
        if (dmaBufTotal > ION_LEAK_THRESHOLD * MBTOKB) {
            reportDmabufLeakException(DMABUF_EVENT_NAME, dmaBufTotal);
            return true;
        }
        return false;
    }

    private boolean checkKgslLeak() {
        if (SCOUT_MEMORY_DISABLE_KGSL) {
            return false;
        }
        long totalKgsl = getTotalKgsl();
        if (debug) {
            Slog.d("MIUIScout Memory", "checkKgslLeak size:" + stringifyKBSize(totalKgsl) + " monitor threshold = " + (KGSL_LEAK_THRESHOLD * MBTOKB) + "kB");
        }
        if (totalKgsl <= KGSL_LEAK_THRESHOLD * MBTOKB) {
            return false;
        }
        reportKgslLeakException(KGSL_EVENT_NAME, totalKgsl);
        return true;
    }

    public void checkScoutLowMemory(ScoutMeminfo scoutMeminfo) {
        if (scoutMeminfo == null || !isEnableScoutMemory()) {
            return;
        }
        if (this.isCameraForeground) {
            Slog.w("MIUIScout Memory", "skip check display memory leak, camera is foreground");
            return;
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "check MIUI Scout display memory(root)");
        }
        if (!checkKgslLeak()) {
            checkDmaBufLeak(scoutMeminfo);
        }
    }

    public void checkScoutLowMemory() {
        if (this.mService == null || !isEnableScoutMemory()) {
            return;
        }
        if (this.isCameraForeground) {
            Slog.w("MIUIScout Memory", "skip check display memory leak, camera is foreground");
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (this.mIsShowDialog.get()) {
            if (now < this.mShowDialogTime + 300000) {
                Slog.w("MIUIScout Memory", "skip check display memory leak, dialog show");
                return;
            }
            setShowDialogState(false);
        }
        if (this.mLastReportTime != 0 && ((this.mDisableState.get() && now < this.mLastReportTime + 600000) || (!isEnableResumeFeature() && now < this.mLastReportTime + SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED))) {
            if (debug) {
                Slog.d("MIUIScout Memory", "mLastReportTime = " + this.mLastReportTime + " mDisableState = " + this.mDisableState.get() + " now = " + now + " MEM_DISABLE_REPORT_INTERVAL = " + MEM_DISABLE_REPORT_INTERVAL + " MEM_REPORT_INTERVAL = " + MEM_REPORT_INTERVAL);
            }
            Slog.d("MIUIScout Memory", "skip check display memory leak, less than last check interval");
            return;
        }
        if (this.mDisableState.get()) {
            this.mDisableState.set(false);
        }
        if (debug) {
            Slog.d("MIUIScout Memory", "check MIUI Scout display memory");
        }
        if (!checkKgslLeak()) {
            checkDmaBufLeak();
        }
    }

    /* loaded from: classes.dex */
    public static class DiaplayMemoryErrorInfo {
        private int action;
        private int adj;
        private int pid;
        private String procName;
        private String reason;
        private long rss;
        private long threshold;
        private long totalSize;
        private int type;

        public DiaplayMemoryErrorInfo(KgslProcUsageInfo kgslInfo, long thresholdSize, long totalSize) {
            this.pid = kgslInfo.getPid();
            this.procName = kgslInfo.getName();
            this.rss = kgslInfo.getRss();
            this.adj = kgslInfo.getOomadj();
            this.threshold = thresholdSize;
            this.totalSize = totalSize;
            this.reason = "kgsl";
            this.type = 2;
        }

        public DiaplayMemoryErrorInfo(DmaBufProcUsageInfo dmabufInfo, long thresholdSize, long totalSize) {
            this.pid = dmabufInfo.getPid();
            this.procName = dmabufInfo.getName();
            this.rss = dmabufInfo.getRss();
            this.adj = dmabufInfo.getOomadj();
            this.threshold = thresholdSize;
            this.totalSize = totalSize;
            this.reason = "DMA-BUF";
            this.type = 1;
        }

        public void setName(String procName) {
            this.procName = procName;
        }

        public String getName() {
            return this.procName;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        public int getPid() {
            return this.pid;
        }

        public void setOomadj(int adj) {
            this.adj = adj;
        }

        public int getOomadj() {
            return this.adj;
        }

        public void setRss(long rss) {
            this.rss = rss;
        }

        public long getRss() {
            return this.rss;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }

        public long getThreshold() {
            return this.threshold;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public long getTotalSize() {
            return this.totalSize;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getReason() {
            return this.reason;
        }

        public void setAction(int action) {
            this.action = action;
        }

        public int getAction() {
            return this.action;
        }

        public int getType() {
            return this.type;
        }
    }
}
