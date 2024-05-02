package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class JavaHeapUsageInfo extends ProcUsageInfo {
    private long javaHeapSize;
    private String stackTrace;

    public long getJavaHeapSize() {
        return this.javaHeapSize;
    }

    public void setJavaHeapSize(long javaHeapSize) {
        this.javaHeapSize = javaHeapSize;
    }

    public String getStackTrace() {
        return this.stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proc info Name: " + getName() + "Pid = " + getPid() + "javaHeap Size = " + this.javaHeapSize + "StackTrace:" + this.stackTrace);
        return sb.toString();
    }
}
