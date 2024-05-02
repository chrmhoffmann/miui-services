package com.miui.server.sentinel;

import java.io.IOException;
/* loaded from: classes.dex */
public class SocketPacket {
    private String data;
    private String event_type;
    private long growsize;
    private int pid;
    private String process_name;
    private int tid;

    public int getPid() {
        return this.pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public long getGrowsize() {
        return this.growsize;
    }

    public void setGrowsize(long growsize) {
        this.growsize = growsize;
    }

    public String getEvent_type() {
        return this.event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public String getProcess_name() {
        return this.process_name;
    }

    public void setProcess_name(String process_name) {
        this.process_name = process_name;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getTid() {
        return this.tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public static long readLong(byte[] buffer) throws IOException {
        long ret = 0;
        for (int i = 0; i <= 7; i++) {
            int tmp = buffer[7 - i] & 255;
            ret += tmp << (56 - (i * 8));
        }
        return ret;
    }

    public static int readInt(byte[] buffer) throws IOException {
        int ret = 0;
        for (int i = 0; i <= 3; i++) {
            int tmp = buffer[3 - i] & 255;
            ret = (int) (ret + (tmp << (24 - (i * 8))));
        }
        return ret;
    }

    public static String readString(byte[] buffer, int start, int len) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < start + len; i++) {
            char c = (char) buffer[i];
            if (c == 0) {
                return sb.toString();
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SocketPacket info Proc Name: " + getProcess_name() + "Pid = " + getPid() + "grow Size = " + getGrowsize() + "event_type = " + getEvent_type() + "data = " + getData());
        return sb.toString();
    }
}
