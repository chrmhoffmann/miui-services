package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class RssUsageInfo extends ProcUsageInfo {
    private String maxIncrease;
    private long rssSize;

    public long getRssSize() {
        return this.rssSize;
    }

    public void setRssSize(long rssSize) {
        this.rssSize = rssSize;
    }

    public String getMaxIncrease() {
        return this.maxIncrease;
    }

    public void setMaxIncrease(String maxIncrease) {
        this.maxIncrease = maxIncrease;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Proc info Name: " + getName() + "Pid = " + getPid() + "Rss Size = " + this.rssSize + " THE MAX INREASE is " + this.maxIncrease);
        return sb.toString();
    }
}
