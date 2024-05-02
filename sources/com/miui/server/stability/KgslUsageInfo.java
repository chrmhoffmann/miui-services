package com.miui.server.stability;

import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class KgslUsageInfo {
    private long nativeTotalSize;
    private long runtimeTotalSize;
    private long totalSize;
    private ArrayList<KgslProcUsageInfo> usageList = new ArrayList<>();

    public void add(KgslProcUsageInfo procInfo) {
        this.usageList.add(procInfo);
    }

    public ArrayList<KgslProcUsageInfo> getList() {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Kgsl info total: " + this.totalSize + "kB RuntimeTotalSize=" + this.runtimeTotalSize + "kB NativeTotalSize=" + this.nativeTotalSize + "kB\n");
        Iterator<KgslProcUsageInfo> it = this.usageList.iterator();
        while (it.hasNext()) {
            KgslProcUsageInfo info = it.next();
            sb.append(info.toString() + "\n");
        }
        return sb.toString();
    }
}
