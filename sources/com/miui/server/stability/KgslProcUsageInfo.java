package com.miui.server.stability;
/* loaded from: classes.dex */
public class KgslProcUsageInfo {
    private long gfxDev;
    private long glMtrack;
    private int oomadj;
    private int pid;
    private String procName;
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

    public void setGfxDev(long gfxDev) {
        this.gfxDev = gfxDev;
    }

    public long getGfxDev() {
        return this.gfxDev;
    }

    public void setGlMtrack(long glMtrack) {
        this.glMtrack = glMtrack;
    }

    public long getGlMtrack() {
        return this.glMtrack;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Kgsl proc info Name: " + this.procName + " Pid=" + this.pid + " Oomadj=" + this.oomadj + " Rss=" + this.rss + "kB GfxDev=" + this.gfxDev + "kB GlMtrack=" + this.glMtrack + "kB");
        return sb.toString();
    }
}
