package com.android.net.module.util.netlink;

import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructNlMsgErr {
    public static final int STRUCT_SIZE = 20;
    public int error;
    public StructNlMsgHdr msg;

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 20;
    }

    public static StructNlMsgErr parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNlMsgErr struct = new StructNlMsgErr();
        struct.error = byteBuffer.getInt();
        struct.msg = StructNlMsgHdr.parse(byteBuffer);
        return struct;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.putInt(this.error);
        StructNlMsgHdr structNlMsgHdr = this.msg;
        if (structNlMsgHdr != null) {
            structNlMsgHdr.pack(byteBuffer);
        }
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("StructNlMsgErr{ error{").append(this.error).append("}, msg{");
        StructNlMsgHdr structNlMsgHdr = this.msg;
        return append.append(structNlMsgHdr == null ? "" : structNlMsgHdr.toString()).append("} }").toString();
    }
}
