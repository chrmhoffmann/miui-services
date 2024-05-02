package com.android.net.module.util.netlink;

import com.android.net.module.util.Struct;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructIfaddrMsg extends Struct {
    public static final int STRUCT_SIZE = 8;
    @Struct.Field(order = 0, type = Struct.Type.U8)
    public final short family;
    @Struct.Field(order = 2, type = Struct.Type.U8)
    public final short flags;
    @Struct.Field(order = 4, type = Struct.Type.S32)
    public final int index;
    @Struct.Field(order = 1, type = Struct.Type.U8)
    public final short prefixLen;
    @Struct.Field(order = 3, type = Struct.Type.U8)
    public final short scope;

    StructIfaddrMsg(short family, short prefixLen, short flags, short scope, int index) {
        this.family = family;
        this.prefixLen = prefixLen;
        this.flags = flags;
        this.scope = scope;
        this.index = index;
    }

    public static StructIfaddrMsg parse(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 8) {
            return null;
        }
        return (StructIfaddrMsg) Struct.parse(StructIfaddrMsg.class, byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        writeToByteBuffer(byteBuffer);
    }
}
