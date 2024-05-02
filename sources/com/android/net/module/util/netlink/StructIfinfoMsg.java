package com.android.net.module.util.netlink;

import com.android.net.module.util.Struct;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructIfinfoMsg extends Struct {
    public static final int STRUCT_SIZE = 16;
    @Struct.Field(order = 4, type = Struct.Type.U32)
    public final long change;
    @Struct.Field(order = 0, padding = 1, type = Struct.Type.U8)
    public final short family;
    @Struct.Field(order = 3, type = Struct.Type.U32)
    public final long flags;
    @Struct.Field(order = 2, type = Struct.Type.S32)
    public final int index;
    @Struct.Field(order = 1, type = Struct.Type.U16)
    public final int type;

    StructIfinfoMsg(short family, int type, int index, long flags, long change) {
        this.family = family;
        this.type = type;
        this.index = index;
        this.flags = flags;
        this.change = change;
    }

    public static StructIfinfoMsg parse(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 16) {
            return null;
        }
        return (StructIfinfoMsg) Struct.parse(StructIfinfoMsg.class, byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        writeToByteBuffer(byteBuffer);
    }
}
