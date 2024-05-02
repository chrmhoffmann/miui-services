package com.miui.server.stability;

import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class DmaBufUsageInfo {
    private long kernelRss;
    private long nativeTotalSize;
    private long runtimeTotalSize;
    private long totalPss;
    private long totalRss;
    private long totalSize;
    private ArrayList<DmaBufProcUsageInfo> usageList = new ArrayList<>();

    public void add(DmaBufProcUsageInfo procInfo) {
        this.usageList.add(procInfo);
    }

    public ArrayList<DmaBufProcUsageInfo> getList() {
        return this.usageList;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public void setRuntimeTotalSize(long runtimeTotalSize) {
        this.runtimeTotalSize = runtimeTotalSize;
    }

    public long getRuntimeTotalSize() {
        return this.runtimeTotalSize;
    }

    public void setNativeTotalSize(long nativeTotalSize) {
        this.nativeTotalSize = nativeTotalSize;
    }

    public long getNativeTotalSize() {
        return this.nativeTotalSize;
    }

    public void setKernelRss(long kernelRss) {
        this.kernelRss = kernelRss;
    }

    public long getKernelRss() {
        return this.kernelRss;
    }

    public void setTotalRss(long totalRss) {
        this.totalRss = totalRss;
    }

    public long getTotalRss() {
        return this.totalRss;
    }

    public void setTotalPss(long totalPss) {
        this.totalPss = totalPss;
    }

    public long getTotalPss() {
        return this.totalPss;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DMA-BUF info total: " + this.totalSize + "kB kernel_rss=" + this.kernelRss + "kB userspace_rss=" + this.totalRss + "kB userspace_pss=" + this.totalPss + "kB RuntimeTotalSize=" + this.runtimeTotalSize + "kB NativeTotalSize=" + this.nativeTotalSize + "kB\n");
        Iterator<DmaBufProcUsageInfo> it = this.usageList.iterator();
        while (it.hasNext()) {
            DmaBufProcUsageInfo info = it.next();
            sb.append(info.toString() + "\n");
        }
        return sb.toString();
    }
}
