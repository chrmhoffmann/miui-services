package com.miui.server.sentinel;
/* loaded from: classes.dex */
public class TrackPacket extends SocketPacket {
    int report_argsz;
    int report_id;
    int report_sz;
    long timestamp;

    public int getReport_id() {
        return this.report_id;
    }

    public void setReport_id(int report_id) {
        this.report_id = report_id;
    }

    public int getReport_argsz() {
        return this.report_argsz;
    }

    public void setReport_argsz(int report_argsz) {
        this.report_argsz = report_argsz;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getReport_sz() {
        return this.report_sz;
    }

    public void setReport_sz(int report_sz) {
        this.report_sz = report_sz;
    }

    @Override // com.miui.server.sentinel.SocketPacket
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Track info Proc info Name: " + getProcess_name() + "Pid = " + getPid() + "data = " + getData());
        return sb.toString();
    }
}
