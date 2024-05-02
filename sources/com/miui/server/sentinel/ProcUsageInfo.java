package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class ProcUsageInfo {
    private int adj;
    private String name;
    private int pid;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPid() {
        return this.pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getAdj() {
        return this.adj;
    }

    public void setAdj(int adj) {
        this.adj = adj;
    }
}
