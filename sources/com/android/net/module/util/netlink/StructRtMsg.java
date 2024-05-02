package com.android.net.module.util.netlink;

import com.android.net.module.util.Struct;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructRtMsg extends Struct {
    public static final int STRUCT_SIZE = 12;
    @Struct.Field(order = 1, type = Struct.Type.U8)
    public final short dstLen;
    @Struct.Field(order = 0, type = Struct.Type.U8)
    public final short family;
    @Struct.Field(order = 8, type = Struct.Type.U32)
    public final long flags;
    @Struct.Field(order = 5, type = Struct.Type.U8)
    public final short protocol;
    @Struct.Field(order = 6, type = Struct.Type.U8)
    public final short scope;
    @Struct.Field(order = 2, type = Struct.Type.U8)
    public final short srcLen;
    @Struct.Field(order = 4, type = Struct.Type.U8)
    public final short table;
    @Struct.Field(order = 3, type = Struct.Type.U8)
    public final short tos;
    @Struct.Field(order = 7, type = Struct.Type.U8)
    public final short type;

    StructRtMsg(short family, short dstLen, short srcLen, short tos, short table, short protocol, short scope, short type, long flags) {
        this.family = family;
        this.dstLen = dstLen;
        this.srcLen = srcLen;
        this.tos = tos;
        this.table = table;
        this.protocol = protocol;
        this.scope = scope;
        this.type = type;
        this.flags = flags;
    }

    public static StructRtMsg parse(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 12) {
            return null;
        }
        return (StructRtMsg) Struct.parse(StructRtMsg.class, byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        writeToByteBuffer(byteBuffer);
    }
}
