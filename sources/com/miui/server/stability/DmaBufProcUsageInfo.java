package com.miui.server.stability;
/* loaded from: classes.dex */
public class DmaBufProcUsageInfo {
    private int oomadj;
    private int pid;
    private String procName;
    private long pss;
    private long rss;

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

    public void setOomadj(int oomadj) {
        this.oomadj = oomadj;
    }

    public int getOomadj() {
        return this.oomadj;
    }

    public void setRss(long rss) {
        this.rss = rss;
    }

    public long getRss() {
        return this.rss;
    }

    public void setPss(long pss) {
        this.pss = pss;
    }

    public long getPss() {
        return this.pss;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DMA-BUF proc info Name: " + this.procName + " Pid=" + this.pid + " Oomadj=" + this.oomadj + " Rss=" + this.rss + "kB Pss=" + this.pss + "kB");
        return sb.toString();
    }
}
