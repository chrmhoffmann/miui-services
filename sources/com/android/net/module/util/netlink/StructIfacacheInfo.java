package com.android.net.module.util.netlink;

import com.android.net.module.util.Struct;
import java.nio.ByteBuffer;
/* loaded from: classes.dex */
public class StructIfacacheInfo extends Struct {
    public static final int STRUCT_SIZE = 16;
    @Struct.Field(order = 2, type = Struct.Type.U32)
    public final long cstamp;
    @Struct.Field(order = 0, type = Struct.Type.U32)
    public final long preferred;
    @Struct.Field(order = 3, type = Struct.Type.U32)
    public final long tstamp;
    @Struct.Field(order = 1, type = Struct.Type.U32)
    public final long valid;

    StructIfacacheInfo(long preferred, long valid, long cstamp, long tstamp) {
        this.preferred = preferred;
        this.valid = valid;
        this.cstamp = cstamp;
        this.tstamp = tstamp;
    }

    public static StructIfacacheInfo parse(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < 16) {
            return null;
        }
        return (StructIfacacheInfo) Struct.parse(StructIfacacheInfo.class, byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        writeToByteBuffer(byteBuffer);
    }
}
