package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class NativeHeapUsageInfo extends ProcUsageInfo {
    private long nativeHeapSize;
    private String stackTrace;

    public long getNativeHeapSize() {
        return this.nativeHeapSize;
    }

    public void setNativeHeapSize(long nativeHeapSize) {
        this.nativeHeapSize = nativeHeapSize;
    }

    public String getStackTrace() {
        return this.stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proc info Name: " + getName() + "Pid = " + getPid() + "NativeHeap Size = " + this.nativeHeapSize + "Track stackTrace" + this.stackTrace);
        return sb.toString();
    }
}
